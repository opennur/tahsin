package com.tahsin.app.data.quran

import android.content.Context
import com.google.gson.Gson
import com.tahsin.app.util.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sumber data mushaf:
 * - Daftar 114 surah: `assets/quran/surah-list.json` (offline).
 * - Isi ayat per surah: diunduh dari equran.id (`v2/surat/{nomor}`) saat surah
 *   pertama kali dibuka, lalu di-cache di `filesDir/quran/surah-<n>.json`
 *   sehingga offline untuk kunjungan berikutnya.
 */
class QuranRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val quranDir: File
        get() = File(appContext.filesDir, "quran")

    @Volatile
    private var cachedList: List<Surah>? = null

    /** Daftar 114 surah (metadata saja, `ayahs` kosong). */
    fun surahList(): List<Surah> = cachedList ?: synchronized(this) {
        cachedList ?: run {
            val json = appContext.assets
                .open("quran/surah-list.json")
                .bufferedReader()
                .use { it.readText() }
            val parsed = gson.fromJson(json, SurahListJson::class.java)
            val list = parsed.surahs.map { it.toSurah() }
            cachedList = list
            list
        }
    }

    /** Surah yang sudah pernah diunduh (teks saja, tanpa terjemahan bahasa lain). */
    fun cachedSurahPlain(number: Int): Surah? {
        val file = File(quranDir, "surah-$number.json")
        if (!file.exists()) return null
        return runCatching { parseSurah(file.readText()) }.getOrNull()
    }

    /**
     * Surah dari cache + terjemahan bahasa aktif. Kalau terjemahan bahasa itu
     * belum ada, diunduh dulu (EN: quran.com; ID: dari respons equran.id).
     * Null kalau isi surah belum pernah diunduh.
     */
    suspend fun cachedSurah(number: Int, lang: AppLanguage): Surah? = withContext(Dispatchers.IO) {
        val file = File(quranDir, "surah-$number.json")
        if (!file.exists()) return@withContext null
        runCatching {
            val raw = file.readText()
            val surah = parseSurah(raw)
            surah.withTranslation(translationFor(number, lang, raw, surah.ayahs.size))
        }.getOrNull()
    }

    /** Unduh surah (equran.id) + terjemahan bahasa aktif, lalu simpan ke cache. */
    suspend fun fetchSurah(number: Int, lang: AppLanguage): Surah = withContext(Dispatchers.IO) {
        val file = File(quranDir, "surah-$number.json")
        val raw: String = if (file.exists()) {
            file.readText()
        } else {
            val text = httpGet("https://equran.id/api/v2/surat/$number")
            file.parentFile?.mkdirs()
            file.writeText(text)
            text
        }
        val surah = parseSurah(raw)
        surah.withTranslation(translationFor(number, lang, raw, surah.ayahs.size))
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            conn.connect()
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ---- terjemahan per bahasa ----

    /** Respons equran.id (mentah) → Surah (teks Arab saja). */
    private fun parseSurah(raw: String): Surah {
        val response = gson.fromJson(raw, SurahResponse::class.java)
        return response.data?.toSurah()
            ?: throw IOException("Respons API kosong.")
    }

    private fun Surah.withTranslation(translations: List<String>): Surah =
        copy(ayahs = ayahs.mapIndexed { i, a -> a.copy(translation = translations.getOrNull(i).orEmpty()) })

    /**
     * Terjemahan per ayat untuk satu bahasa.
     * - ID: `teksIndonesia` dari respons equran.id (sudah tersimpan di cache surah).
     * - EN: quran.com API (Saheeh International, resource 20), di-cache
     *   di `trans-en-<n>.json` agar offline untuk kunjungan berikutnya.
     */
    private suspend fun translationFor(
        number: Int,
        lang: AppLanguage,
        rawEquran: String,
        ayahCount: Int,
    ): List<String> {
        if (lang == AppLanguage.ID) {
            val dto = gson.fromJson(rawEquran, SurahResponse::class.java) ?: return emptyList()
            return dto.data?.ayat?.map { it.teksIndonesia } ?: emptyList()
        }
        val file = File(quranDir, "trans-en-$number.json")
        if (file.exists()) {
            val cached = runCatching {
                gson.fromJson(file.readText(), TranslationListJson::class.java)?.translations?.map { stripHtml(it.text) }
            }.getOrNull()
            if (cached != null && cached.size == ayahCount) return cached
        }
        val json = httpGet("https://api.quran.com/api/v4/quran/translations/20?chapter_number=$number")
        val parsed = gson.fromJson(json, TranslationListJson::class.java) ?: return emptyList()
        // Simpan versi SUDAH dibersihkan agar cache tidak menyimpan tag HTML.
        val cleaned = parsed.translations.map { it.copy(text = stripHtml(it.text)) }
        val texts = cleaned.map { it.text }
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(TranslationListJson(cleaned)))
        return texts
    }

    /**
     * Buang tag HTML dari quran.com, termasuk footnote `<sup ...>N</sup>`
     * (tag + nomornya), lalu rapikan spasi.
     */
    private fun stripHtml(s: String): String =
        s.replace(Regex("(?i)<sup[^>]*>.*?</sup>"), "")   // footnote utuh (tag + nomor)
            .replace(Regex("<[^>]*>"), "")                 // tag lain
            .replace(Regex("\\s+"), " ")
            .trim()

    // ---- DTO (JSON) ----

    private data class SurahListJson(val surahs: List<SurahMetaJson> = emptyList())

    private data class SurahMetaJson(
        val number: Int = 0,
        val nameArabic: String = "",
        val nameLatin: String = "",
        val ayahCount: Int = 0,
    ) {
        fun toSurah() = Surah(number, nameArabic, nameLatin, ayahCount)
    }

    private data class SurahResponse(val data: SurahData? = null)

    private data class SurahData(
        val nomor: Int = 0,
        val nama: String = "",
        val namaLatin: String = "",
        val jumlahAyat: Int = 0,
        val ayat: List<AyahData> = emptyList(),
    ) {
        fun toSurah() = Surah(
            number = nomor,
            nameArabic = nama,
            nameLatin = namaLatin,
            ayahCount = jumlahAyat,
            ayahs = ayat.map { Ayah(it.nomorAyat, it.teksArab) },
        )
    }

    private data class AyahData(
        val nomorAyat: Int = 0,
        val teksArab: String = "",
        val teksIndonesia: String = "",
    )

    private data class TranslationListJson(
        val translations: List<TranslationItemJson> = emptyList(),
    )

    private data class TranslationItemJson(
        val resource_id: Int = 0,
        val text: String = "",
    )
}
