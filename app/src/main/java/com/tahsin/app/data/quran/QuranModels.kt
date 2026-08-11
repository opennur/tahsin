package com.tahsin.app.data.quran

import com.tahsin.app.util.ArabicNormalizer

/** Seluruh mushaf yang di-bundle di assets (subset surah). */
data class Mushaf(
    val surahs: List<Surah> = emptyList(),
)

/** Satu surah lengkap. */
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val ayahs: List<Ayah> = emptyList(),
)

/** Satu ayat. */
data class Ayah(
    val number: Int,
    val text: String,
    val audioUrl: String? = null,
) {
    /** Kata-kata ayat (token yang mengandung huruf Arab; penanda waqaf dibuang). */
    val words: List<String>
        get() = ArabicNormalizer.splitWords(text)
}
