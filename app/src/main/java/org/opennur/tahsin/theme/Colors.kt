package org.opennur.tahsin.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Palet warna kustom "Tahsin Quran" — hangat, islami, tanpa Material 3.
 *
 * Mendukung mode terang & gelap: ubah `isDark` (state Compose), semua
 * komponen yang membaca `AyahColors.*` otomatis ikut recompose.
 *
 * - `SurfaceVariant`: latar halus untuk field/dropdown/chip.
 * - `Hairline`: garis tipis untuk batas lembut (gaya modern flat).
 * - `PrimarySoft`: tint lembut warna brand untuk state terpilih.
 */
object AyahColors {

    /** Mode gelap — diubah lewat toggle di aplikasi (persisted di SettingsStore). */
    var isDark by mutableStateOf(false)

    private val Light = Palette(
        Primary = Color(0xFF2D7D6B),
        PrimaryLight = Color(0xFF4CAF8C),
        PrimarySoft = Color(0xFFE2F0EB),
        Secondary = Color(0xFFC49A6C),
        Background = Color(0xFFF7F3EE),
        Surface = Color(0xFFFFFFFF),
        SurfaceVariant = Color(0xFFF0EBE2),
        Divider = Color(0xFFE5DED5),
        Hairline = Color(0xFFDDD5C8),
        TextPrimary = Color(0xFF1A1A1A),
        TextSecondary = Color(0xFF5A5A5A),
        Error = Color(0xFFD32F2F),
        Success = Color(0xFF388E3C),
        Reading = Color(0xFFE8C47A),
        OnPrimary = Color(0xFFFFFFFF),
        OnSecondary = Color(0xFF1A1A1A),
        OnReading = Color(0xFF1A1A1A),
    )

    private val Dark = Palette(
        Primary = Color(0xFF4CAF8C),
        PrimaryLight = Color(0xFF66C9A5),
        PrimarySoft = Color(0xFF1E3A31),
        Secondary = Color(0xFFD0A675),
        Background = Color(0xFF141412),
        Surface = Color(0xFF1F1F1C),
        SurfaceVariant = Color(0xFF282823),
        Divider = Color(0xFF35352F),
        Hairline = Color(0xFF3E3E37),
        TextPrimary = Color(0xFFECEAE4),
        TextSecondary = Color(0xFFA8A59B),
        Error = Color(0xFFEF5350),
        Success = Color(0xFF66BB6A),
        Reading = Color(0xFFC89B5A),
        OnPrimary = Color(0xFF10201A),
        OnSecondary = Color(0xFF1A1A1A),
        OnReading = Color(0xFF1A1A1A),
    )

    // Brand
    val Primary get() = palette().Primary
    val PrimaryLight get() = palette().PrimaryLight
    val PrimarySoft get() = palette().PrimarySoft
    val Secondary get() = palette().Secondary

    // Background & surface
    val Background get() = palette().Background
    val Surface get() = palette().Surface
    val SurfaceVariant get() = palette().SurfaceVariant
    val Divider get() = palette().Divider
    val Hairline get() = palette().Hairline

    // Text
    val TextPrimary get() = palette().TextPrimary
    val TextSecondary get() = palette().TextSecondary

    // Status — dipakai untuk highlight bacaan
    val Error get() = palette().Error
    val Success get() = palette().Success
    val Reading get() = palette().Reading

    // Warna huruf mushaf (tajwid) — gaya mushaf tajwid berwarna
    val TajwidMad get() = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)          // merah: mad
    val TajwidGhunnah get() = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)     // hijau: ghunnah
    val TajwidQalqalah get() = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)    // biru: qalqalah
    val TajwidIkhfa get() = if (isDark) Color(0xFF90A4AE) else Color(0xFF546E7A)       // abu-abu: ikhfa
    val TajwidIqlab get() = if (isDark) Color(0xFFBA68C8) else Color(0xFF6A1B9A)       // ungu: iqlab
    val TajwidIdgham get() = if (isDark) Color(0xFFFFA726) else Color(0xFFE65100)      // oranye: idgham
    val TajwidLamJalalah get() = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00695C)  // teal: lam jalalah

    // Teks di atas warna brand
    val OnPrimary get() = palette().OnPrimary
    val OnSecondary get() = palette().OnSecondary
    val OnReading get() = palette().OnReading

    private fun palette(): Palette = if (isDark) Dark else Light
}

/** Palet internal (terang/gelap). */
private data class Palette(
    val Primary: Color,
    val PrimaryLight: Color,
    val PrimarySoft: Color,
    val Secondary: Color,
    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val Divider: Color,
    val Hairline: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Error: Color,
    val Success: Color,
    val Reading: Color,
    val OnPrimary: Color,
    val OnSecondary: Color,
    val OnReading: Color,
)
