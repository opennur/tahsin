package com.tahsin.app.data.quran

import com.tahsin.app.util.ArabicNormalizer

/**
 * Satu surah: metadata selalu tersedia (dari `surah-list.json`),
 * `ayahs` diisi saat isi surah diunduh/di-cache (equran.id).
 */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val ayahCount: Int = 0,
    val ayahs: List<Ayah> = emptyList(),
)

/** Satu ayat. */
data class Ayah(
    val number: Int,
    val text: String,
) {
    /** Kata-kata ayat (token yang mengandung huruf Arab; penanda waqaf dibuang). */
    val words: List<String>
        get() = ArabicNormalizer.splitWords(text)
}
