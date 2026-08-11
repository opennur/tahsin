package com.tahsin.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Custom design system "Tahsin Quran" — sengaja TANPA Material 3.
 *
 * Warna disediakan lewat CompositionLocal sehingga bisa di-override
 * di masa depan (mis. mode gelap / tema tambahan).
 */
val LocalAyahColors = staticCompositionLocalOf { AyahColors }

/** Shape kustom design system — sudut lembut & ramping. */
object AyahShapes {
    val Button = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(14.dp)
    val Modal = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(10.dp)
}

@Composable
fun AyahTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAyahColors provides AyahColors,
        content = content,
    )
}
