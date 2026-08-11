package com.tahsin.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * Tipografi kustom (tanpa Material 3).
 *
 * PENTING: semua style memakai `get()` — warna dibaca saat komposisi, bukan
 * saat inisialisasi object. Ini yang membuat dark mode bisa mengubah warna
 * teks secara real-time (AyahColors.* adalah state Compose).
 *
 * Semua ukuran memakai sp agar mengikuti skala font sistem (accessibility).
 */
object AyahTypography {
    /** 28sp Bold */
    val Heading1: TextStyle
        get() = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp,
            color = AyahColors.TextPrimary,
        )

    /** 22sp Semibold */
    val Heading2: TextStyle
        get() = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp,
            color = AyahColors.TextPrimary,
        )

    /** 16sp Regular, line height 1.5x */
    val Body1: TextStyle
        get() = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = AyahColors.TextPrimary,
        )

    /** 14sp Regular */
    val Body2: TextStyle
        get() = TextStyle(
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = AyahColors.TextPrimary,
        )

    /** 12sp Regular */
    val Caption: TextStyle
        get() = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AyahColors.TextSecondary,
        )

    /** Teks Arab — RTL, rata kanan, besar agar mudah dibaca */
    val Arabic: TextStyle
        get() = TextStyle(
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 46.sp,
            textAlign = TextAlign.End,
            textDirection = TextDirection.Rtl,
            color = AyahColors.TextPrimary,
        )

    /** Kata Arab dalam chip mushaf — lebih kecil dari ayat utuh */
    val ArabicWord: TextStyle
        get() = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl,
            color = AyahColors.TextPrimary,
        )

    /** Tombol (warna ditetapkan oleh varian tombol) */
    val Button: TextStyle
        get() = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        )

    /** Transliterasi miring */
    val Transliteration: TextStyle
        get() = TextStyle(
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 21.sp,
            color = AyahColors.TextSecondary,
        )
}
