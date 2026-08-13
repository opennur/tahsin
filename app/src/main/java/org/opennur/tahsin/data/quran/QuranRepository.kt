package org.opennur.tahsin.data.quran

import android.content.Context
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.SearchableAyah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sumber data mushaf:
 * - Daftar 114 surah: `assets/quran/surah-list.json` (offline).
 * - Isi ayat + terjemahan ID: `assets/quran/data/surah-<n>.json` (hasil
 *   `tools/fetch_quran_data.py`, di-bundle ke APK → offline siap pakai).
 * - Terjemahan EN: `assets/quran/data/trans-en-<n>.json` (bundle sama).
 * - Fallback: kalau aset belum ada (script belum dijalankan), diunduh dari
 *   equran.id / quran.com lalu di-cache di `filesDir/quran/`.
 *
 * Parsing JSON (murni JVM, bisa di-unit-test) ada di [QuranParser].
 */
class QuranRepository(context: Context) {

    private val appContext = context.applicationContext
    private val quranDir: File
        get() = File(appContext.filesDir, "quran")

    @Volatile
    private var cachedList: List<Surah>? = null

    @Volatile
    private var cachedPages: MushafPagination? = null

    /** Daftar 114 surah (metadata saja, `ayahs` kosong). */
    fun surahList(): List<Surah> = cachedList ?: synchronized(this) {
        cachedList ?: run {
            val json = appContext.assets
                .open("quran/surah-list.json")
                .bufferedReader()
                .use { it.readText() }
            val list = QuranParser.parseSurahList(json)
            cachedList = list
            list
        }
    }

    /** Paginasi mushaf Madani (604 halaman + 30 juz) dari bundle aset. */
    fun pagination(): MushafPagination = cachedPages ?: synchronized(this) {
        cachedPages ?: run {
            val json = appContext.assets
                .open("quran/pages.json")
                .bufferedReader()
                .use { it.readText() }
            val pages = MushafPagesParser.parse(json)
            cachedPages = pages
            pages
        }
    }

    /** Isi surah (mentah equran.id) dari bundle aset, kalau ada. */
    private fun assetSurahRaw(number: Int): String? =
        runCatching {
            appContext.assets.open("quran/data/surah-$number.json")
                .bufferedReader().use { it.readText() }
        }.getOrNull()

    /** Terjemahan EN (mentah, format TranslationListJson) dari bundle aset. */
    private fun assetEnRaw(number: Int): String? =
        runCatching {
            appContext.assets.open("quran/data/trans-en-$number.json")
                .bufferedReader().use { it.readText() }
        }.getOrNull()

    /** Surah yang sudah tersedia (bundle aset / cache) — teks saja. */
    fun cachedSurahPlain(number: Int): Surah? {
        assetSurahRaw(number)?.let { return runCatching { QuranParser.parseSurah(it) }.getOrNull() }
        val file = File(quranDir, "surah-$number.json")
        if (!file.exists()) return null
        return runCatching { QuranParser.parseSurah(file.readText()) }.getOrNull()
    }

    /**
     * Surah dari cache + terjemahan bahasa aktif. Kalau terjemahan bahasa itu
     * belum ada, diunduh dulu (EN: quran.com; ID: dari respons equran.id).
     * Null kalau isi surah belum pernah diunduh.
     */
    suspend fun cachedSurah(number: Int, lang: AppLanguage): Surah? = withContext(Dispatchers.IO) {
        val raw = assetSurahRaw(number)
            ?: runCatching { File(quranDir, "surah-$number.json").readText() }.getOrNull()
            ?: return@withContext null
        runCatching {
            val surah = QuranParser.parseSurah(raw)
            surah.withTranslation(translationFor(number, lang, raw, surah.ayahs.size))
        }.getOrNull()
    }

    /** Unduh surah (equran.id) + terjemahan bahasa aktif, lalu simpan ke cache. */
    suspend fun fetchSurah(number: Int, lang: AppLanguage): Surah = withContext(Dispatchers.IO) {
        val raw = assetSurahRaw(number)
            ?: runCatching { File(quranDir, "surah-$number.json").readText() }.getOrNull()
            ?: httpGet("https://equran.id/api/v2/surat/$number").also { text ->
                File(quranDir, "surah-$number.json").apply {
                    parentFile?.mkdirs()
                    writeText(text)
                }
            }
        val surah = QuranParser.parseSurah(raw)
        surah.withTranslation(translationFor(number, lang, raw, surah.ayahs.size))
    }

    /**
     * Indeks pencarian seluruh mushaf: teks Arab + terjemahan ID & EN untuk
     * setiap ayat, dibangun dari bundle aset (offline). Surah yang kontennya
     * belum tersedia dilewati tanpa crash. Dipanggil sekali lalu di-cache di
     * pemanggil (SearchViewModel) — 114 file JSON, jadi jangan per-keystroke.
     */
    suspend fun searchIndex(): List<SearchableAyah> = withContext(Dispatchers.IO) {
        surahList().mapNotNull { meta ->
            val n = meta.number
            val raw = assetSurahRaw(n)
                ?: runCatching { File(quranDir, "surah-$n.json").readText() }.getOrNull()
                ?: return@mapNotNull null
            runCatching {
                val surah = QuranParser.parseSurah(raw)
                val idTr = QuranParser.parseIdTranslations(raw)
                val enRaw = assetEnRaw(n)
                    ?: runCatching { File(quranDir, "trans-en-$n.json").readText() }.getOrNull()
                val enTr = enRaw
                    ?.let { runCatching { QuranParser.parseEnTranslations(it) }.getOrNull() }
                    .orEmpty()
                surah.ayahs.mapIndexed { i, a ->
                    SearchableAyah(
                        surahNumber = n,
                        ayahNumber = a.number,
                        arabic = a.text,
                        translationId = idTr.getOrNull(i).orEmpty(),
                        translationEn = enTr.getOrNull(i).orEmpty(),
                    )
                }
            }.getOrNull().orEmpty()
        }.flatten()
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
            return QuranParser.parseIdTranslations(rawEquran)
        }
        val file = File(quranDir, "trans-en-$number.json")
        val rawEn = assetEnRaw(number) ?: runCatching { file.readText() }.getOrNull()
        if (rawEn != null) {
            val cached = runCatching { QuranParser.parseEnTranslations(rawEn) }.getOrNull()
            if (cached != null && cached.size == ayahCount) return cached
        }
        val json = httpGet("https://api.quran.com/api/v4/quran/translations/20?chapter_number=$number")
        val texts = QuranParser.parseEnTranslations(json)
        // Simpan versi SUDAH dibersihkan agar cache tidak menyimpan tag HTML.
        QuranParser.buildEnCacheJson(json)?.let { cache ->
            file.parentFile?.mkdirs()
            file.writeText(cache)
        }
        return texts
    }
}
