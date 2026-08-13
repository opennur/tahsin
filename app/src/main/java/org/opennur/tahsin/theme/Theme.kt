package org.opennur.tahsin.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

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
    // Ikon system bars mengikuti mode gelap aplikasi (edge-to-edge, targetSdk 35).
    // Dibaca saat komposisi supaya recompose saat isDark berubah.
    val isDark = AyahColors.isDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }
    CompositionLocalProvider(
        LocalAyahColors provides AyahColors,
        content = content,
    )
}
