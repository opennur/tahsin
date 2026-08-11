package com.ayahofday.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * Tipografi kustom (tanpa Material 3).
 * Semua ukuran memakai sp agar mengikuti skala font sistem (accessibility).
 *
 * TODO: pasang font Inter/Poppins (TTF di res/font) lalu set `fontFamily`
 *  pada tiap style. Saat ini memakai font default sistem.
 */
object AyahTypography {
    /** 28sp Bold */
    val Heading1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        color = AyahColors.TextPrimary,
    )

    /** 22sp Semibold */
    val Heading2 = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        color = AyahColors.TextPrimary,
    )

    /** 16sp Regular, line height 1.5x */
    val Body1 = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = AyahColors.TextPrimary,
    )

    /** 14sp Regular */
    val Body2 = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = AyahColors.TextPrimary,
    )

    /** 12sp Regular */
    val Caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = AyahColors.TextSecondary,
    )

    /** Teks Arab — RTL, rata kanan, besar agar mudah dibaca */
    val Arabic = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 46.sp,
        textAlign = TextAlign.End,
        textDirection = TextDirection.Rtl,
        color = AyahColors.TextPrimary,
    )

    /** Tombol */
    val Button = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    )

    /** Transliterasi miring */
    val Transliteration = TextStyle(
        fontSize = 14.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 21.sp,
        color = AyahColors.TextSecondary,
    )
}
