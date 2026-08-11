package com.tahsin.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * Tipografi kustom (tanpa Material 3) — ringkas & modern.
 *
 * PENTING: semua style memakai `get()` — warna dibaca saat komposisi, bukan
 * saat inisialisasi object (supaya dark mode bisa mengubah warna teks).
 *
 * Semua ukuran memakai sp agar mengikuti skala font sistem (accessibility).
 */
object AyahTypography {
    /** 24sp Bold — judul layar */
    val Heading1: TextStyle
        get() = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
            color = AyahColors.TextPrimary,
        )

    /** 19sp Semibold — judul seksi / panel */
    val Heading2: TextStyle
        get() = TextStyle(
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.sp,
            color = AyahColors.TextPrimary,
        )

    /** 15sp Regular — teks utama */
    val Body1: TextStyle
        get() = TextStyle(
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = AyahColors.TextPrimary,
        )

    /** 14sp Regular — teks sekunder */
    val Body2: TextStyle
        get() = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = AyahColors.TextPrimary,
        )

    /** 12sp Regular — keterangan kecil */
    val Caption: TextStyle
        get() = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AyahColors.TextSecondary,
        )

    /** 11sp Medium, letter-spacing — label seksi (gaya overline) */
    val Overline: TextStyle
        get() = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            color = AyahColors.TextSecondary,
        )

    /** Teks Arab — RTL, rata kanan, besar agar mudah dibaca */
    val Arabic: TextStyle
        get() = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp,
            textAlign = TextAlign.End,
            textDirection = TextDirection.Rtl,
            color = AyahColors.TextPrimary,
        )

    /** Kata Arab dalam chip mushaf */
    val ArabicWord: TextStyle
        get() = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl,
            color = AyahColors.TextPrimary,
        )

    /** Tombol (warna ditetapkan oleh varian tombol) */
    val Button: TextStyle
        get() = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )

    /** Transliterasi miring */
    val Transliteration: TextStyle
        get() = TextStyle(
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            lineHeight = 20.sp,
            color = AyahColors.TextSecondary,
        )
}
