package com.tahsin.app.data.quran

import android.content.Context
import com.google.gson.Gson
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

    /** Surah yang sudah pernah diunduh (dari cache). Null kalau belum pernah. */
    fun cachedSurah(number: Int): Surah? {
        val file = File(quranDir, "surah-$number.json")
        if (!file.exists()) return null
        return runCatching {
            gson.fromJson(file.readText(), SurahResponse::class.java)?.data?.toSurah()
        }.getOrNull()
    }

    /** Unduh surah dari equran.id lalu simpan ke cache. */
    suspend fun fetchSurah(number: Int): Surah = withContext(Dispatchers.IO) {
        val text = httpGet("https://equran.id/api/v2/surat/$number")
        val response = gson.fromJson(text, SurahResponse::class.java)
        val surah = response.data?.toSurah()
            ?: throw IOException("Respons API kosong untuk surah $number.")
        File(quranDir, "surah-$number.json").apply {
            parentFile?.mkdirs()
            writeText(text)
        }
        surah
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

    private data class AyahData(val nomorAyat: Int = 0, val teksArab: String = "")
}
