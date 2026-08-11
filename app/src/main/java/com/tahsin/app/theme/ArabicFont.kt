package com.tahsin.app.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Pilihan jenis font untuk teks Arab mushaf.
 *
 * Saat ini memakai font sistem Android (Noto Naskh Arabic via fallback).
 * TODO: bundle font mushaf sungguhan (mis. Amiri Quran / Scheherazade New,
 * keduanya gratis) ke `res/font/` lalu daftarkan di sini dengan
 * `FontFamily(Font(R.font.amiri_quran))`.
 */
enum class ArabicFont(
    val label: String,
    val family: FontFamily,
) {
    SYSTEM("Font bawaan", FontFamily.Default),
    SERIF("Serif", FontFamily.Serif),
    MONOSPACE("Monospace", FontFamily.Monospace),
}
