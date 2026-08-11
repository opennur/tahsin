package com.ayahofday.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet warna kustom "Ayah of the Day" — hangat dan islami, tanpa Material 3.
 *
 * Pasangan warna teks/latar dirancang dengan kontras >= 4.5:1 (WCAG AA)
 * agar nyaman dibaca semua usia.
 */
object AyahColors {
    // Brand
    val Primary = Color(0xFF2D7D6B)       // hijau Islami menenangkan
    val PrimaryLight = Color(0xFF4CAF8C)
    val Secondary = Color(0xFFC49A6C)     // emas/beige hangat

    // Background & surface
    val Background = Color(0xFFF7F3EE)    // putih hangat, tidak silau
    val Surface = Color(0xFFFFFFFF)
    val Divider = Color(0xFFE5DED5)

    // Text
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF5A5A5A)

    // Status
    val Error = Color(0xFFD32F2F)
    val Success = Color(0xFF388E3C)

    // Teks di atas warna brand
    val OnPrimary = Color(0xFFFFFFFF)
    // Teks gelap dipakai di atas Secondary (gold) karena kontras putih < 4.5:1.
    val OnSecondary = Color(0xFF1A1A1A)
}
