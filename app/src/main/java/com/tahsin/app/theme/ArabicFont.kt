package com.tahsin.app.theme

/**
 * Pilihan jenis font untuk teks Arab mushaf.
 *
 * - UTSMANI: gaya khat Utsmani (default). Font nyata diunduh otomatis dari
 *   Google Fonts (Amiri, lisensi OFL) ke filesDir/fonts; sebelum ada, memakai
 *   font sistem (Noto Naskh Arabic — gaya Utsmani).
 * - INDOPAK: gaya mushaf Indopak. Belum ada sumber otomatis yang terverifikasi;
 *   taruh TTF di filesDir/fonts/indopak.ttf untuk mengaktifkannya (fallback ke
 *   font sistem selama file belum ada).
 * - ANDROID: font sistem perangkat.
 */
enum class ArabicFont(
    val label: String,
    /** Nama file di filesDir/fonts (null = selalu font sistem). */
    val fileName: String?,
    /** URL unduhan font (null = tidak ada sumber otomatis). */
    val downloadUrl: String?,
) {
    UTSMANI(
        label = "Utsmani (default)",
        fileName = "uthmani.ttf",
        downloadUrl = "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf",
    ),
    INDOPAK(
        label = "Indopak",
        fileName = "indopak.ttf",
        downloadUrl = null,
    ),
    ANDROID(
        label = "Android",
        fileName = null,
        downloadUrl = null,
    ),
}
