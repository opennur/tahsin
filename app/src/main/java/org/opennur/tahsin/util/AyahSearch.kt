package org.opennur.tahsin.util

import java.util.Locale

/**
 * Satu ayat lengkap untuk pencarian: teks Arab + terjemahan Indonesia & Inggris
 * (keduanya tersedia offline dari bundle aset).
 */
data class SearchableAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabic: String,
    val translationId: String,
    val translationEn: String,
)

/**
 * Logika pencarian ayat (murni, tanpa Android — bisa diuji JVM):
 * - Kata Arab: query dinormalisasi dengan [ArabicNormalizer] (buang harakat,
 *   samakan hamza/ya/ta marbuta) lalu dicocokkan substring / awalan kata.
 * - Kata kunci terjemahan: substring case-insensitive pada terjemahan ID/EN.
 */
object AyahSearch {

    private const val DEFAULT_LIMIT = 40

    /** Apakah satu ayat cocok dengan query (Arab / terjemahan ID / terjemahan EN). */
    fun matches(arabic: String, translationId: String, translationEn: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false

        val normQuery = searchNormalize(q)
        if (normQuery.isNotEmpty() && arabicMatches(arabic, normQuery)) return true

        val lq = q.lowercase(Locale.ROOT)
        if (translationId.lowercase(Locale.ROOT).contains(lq)) return true
        if (translationEn.lowercase(Locale.ROOT).contains(lq)) return true
        return false
    }

    /**
     * Cari di indeks; hasil diurutkan (surah, ayat) dan dibatasi.
     * Query kosong → daftar kosong.
     */
    fun search(index: List<SearchableAyah>, query: String, limit: Int = DEFAULT_LIMIT): List<SearchableAyah> {
        val q = query.trim()
        if (q.isEmpty() || limit <= 0) return emptyList()
        return index.asSequence()
            .filter { matches(it.arabic, it.translationId, it.translationEn, q) }
            .sortedWith(compareBy<SearchableAyah> { it.surahNumber }.thenBy { it.ayahNumber })
            .take(limit)
            .toList()
    }

    /** Substring pada teks Arab ternormalisasi, atau awalan kata (mis. "رحم" → "الرحمن"). */
    private fun arabicMatches(arabic: String, normQuery: String): Boolean {
        val normText = searchNormalize(arabic)
        if (normText.contains(normQuery)) return true
        return ArabicNormalizer.splitWords(normText).any { it.startsWith(normQuery) }
    }

    /**
     * Normalisasi KHUSUS pencarian: [ArabicNormalizer.normalize] ditambah lipat
     * hamza berkursi (ؤ → و, ئ → ي) supaya "مومن" tetap ketemu "مُؤْمِنَ".
     * Sengaja TIDAK mengubah ArabicNormalizer (dipakai juga untuk STT).
     */
    private fun searchNormalize(text: String): String =
        ArabicNormalizer.normalize(text).replace('ؤ', 'و').replace('ئ', 'ي')
}
