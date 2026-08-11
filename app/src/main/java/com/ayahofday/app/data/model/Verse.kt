package com.ayahofday.app.data.model

/**
 * Domain model satu ayat.
 * Implementasi sumber data (EQuran.id API / Kemenag) akan diisi kemudian.
 */
data class Verse(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val tafsir: String? = null,
    val asbabunNuzul: String? = null,
) {
    /** Contoh: "Al-Fatihah 1:1" */
    val reference: String
        get() = "$surahName $surahNumber:$ayahNumber"

    /** Teks siap dibagikan (WhatsApp). */
    fun toShareText(): String = buildString {
        appendLine("🕌 *Ayah of the Day*")
        appendLine(reference)
        appendLine()
        appendLine(arabic)
        appendLine(transliteration)
        appendLine(translation)
    }
}
