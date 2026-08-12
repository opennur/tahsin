package com.tahsin.app.data.quran

import com.google.gson.Gson
import java.io.IOException

/**
 * Parsing JSON mushaf (equran.id + quran.com) — murni JVM, tanpa Context,
 * supaya bisa di-unit-test tanpa Android. I/O (assets/cache/network) tetap
 * ditangani [QuranRepository]; parser ini hanya JSON → model.
 *
 * Format yang dipahami:
 * - `surah-list.json`           → metadata 114 surah
 * - `surah-<n>.json`            → respons mentah equran.id (Arab + terjemahan ID)
 * - `trans-en-<n>.json`         → respons quran.com resource 20 (Saheeh Int'l)
 */
object QuranParser {

    private val gson = Gson()

    /** Daftar 114 surah (metadata saja, `ayahs` kosong). */
    fun parseSurahList(json: String): List<Surah> {
        val parsed = gson.fromJson(json, SurahListJson::class.java)
        return parsed.surahs.map { it.toSurah() }
    }

    /** Respons equran.id (mentah) → [Surah] (teks Arab saja). */
    @Throws(IOException::class)
    fun parseSurah(raw: String): Surah {
        val response = gson.fromJson(raw, SurahResponse::class.java)
        return response.data?.toSurah() ?: throw IOException("Respons API kosong.")
    }

    /**
     * Terjemahan Indonesia per ayat dari respons equran.id (`teksIndonesia`).
     * JSON malformed (mis. cache rusak / respons API aneh) → daftar kosong,
     * bukan crash — pemanggil menurunkan kualitas secara halus.
     */
    fun parseIdTranslations(rawEquran: String): List<String> {
        val dto = runCatching { gson.fromJson(rawEquran, SurahResponse::class.java) }.getOrNull()
            ?: return emptyList()
        return dto.data?.ayat?.map { it.teksIndonesia } ?: emptyList()
    }

    /**
     * Terjemahan EN (quran.com, resource 20) — tag HTML & footnote `<sup>` sudah
     * dibersihkan. JSON malformed → daftar kosong (tidak crash).
     */
    fun parseEnTranslations(json: String): List<String> {
        val parsed = runCatching { gson.fromJson(json, TranslationListJson::class.java) }.getOrNull()
            ?: return emptyList()
        return parsed.translations.map { stripHtml(it.text) }
    }

    /**
     * Versi cache EN (SUDAH dibersihkan dari tag HTML) sebagai JSON berformat
     * `TranslationListJson` — supaya file cache tidak menyimpan tag.
     * Null kalau JSON tidak bisa di-parse (pemanggil tidak menulis cache).
     */
    fun buildEnCacheJson(json: String): String? {
        val parsed = runCatching { gson.fromJson(json, TranslationListJson::class.java) }.getOrNull()
            ?: return null
        val cleaned = parsed.translations.map { it.copy(text = stripHtml(it.text)) }
        return gson.toJson(TranslationListJson(cleaned))
    }

    /**
     * Buang tag HTML dari quran.com, termasuk footnote `<sup ...>N</sup>`
     * (tag + nomornya), lalu rapikan spasi.
     */
    fun stripHtml(s: String): String =
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
