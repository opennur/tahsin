package com.tahsin.app.data.tajwid

import com.tahsin.app.util.ArabicNormalizer

/**
 * Rentang karakter (di dalam kata asli ber-tashkeel) yang diwarnai satu
 * kategori hukum tajwid. `end` eksklusif; tanda harakat yang menempel pada
 * huruf ikut terwarnai.
 */
data class TajwidSpan(
    val start: Int,
    val end: Int,
    val category: RuleCategory,
)

/**
 * Mengubah daftar [TajwidRule] menjadi span warna per huruf, untuk
 * pewarnaan mushaf (gaya mushaf tajwid warna).
 */
object TajwidColorizer {

    /** Kategori yang tidak diwarnai (dibaca biasa / tidak punya warna khas). */
    private val UNCOLORED = setOf(RuleCategory.IZHAR, RuleCategory.SUKUN, RuleCategory.SHADDAH)

    /**
     * Bangun span warna untuk satu kata. Satu huruf hanya boleh satu warna
     * (rule terakhir menang). Huruf tanpa hukum tidak ikut diwarnai.
     */
    fun spans(word: String, rules: List<TajwidRule>): List<TajwidSpan> {
        if (rules.isEmpty()) return emptyList()
        val byLetter = sortedMapOf<Int, RuleCategory>()
        for (r in rules) {
            if (r.category in UNCOLORED) continue
            byLetter[r.letterIndex] = r.category
        }
        if (byLetter.isEmpty()) return emptyList()
        val result = mutableListOf<TajwidSpan>()
        byLetter.forEach { (idx, cat) ->
            var end = idx + 1
            while (end < word.length && !ArabicNormalizer.isLetter(word[end])) end++
            result += TajwidSpan(start = idx, end = end, category = cat)
        }
        return result
    }
}
