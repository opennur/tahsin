package com.tahsin.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahShapes
import com.tahsin.app.theme.AyahTypography

enum class AyahButtonVariant {
    Primary,
    Secondary,
    Outline,
    /** Tanpa latar & tanpa garis — aksi sekunder/ikon halus. */
    Ghost,
    /** Merah — aksi berbahaya/menghentikan (mis. Stop). */
    Danger,
}

enum class AyahButtonSize {
    /** Standar: tinggi 40dp. */
    Default,
    /** Kompak: tinggi 32dp (navigasi ‹ ›, ikon kecil). */
    Small,
}

/**
 * Tombol kustom (tanpa Material 3) — ramping & modern:
 * - Tinggi 40dp (Default) / 32dp (Small), touch target tetap nyaman.
 * - Sudut 10dp, border tipis 1dp untuk varian Outline.
 * - Varian: Primary, Secondary (gold), Outline (garis), Ghost (halus).
 */
@Composable
fun AyahButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AyahButtonVariant = AyahButtonVariant.Primary,
    enabled: Boolean = true,
    size: AyahButtonSize = AyahButtonSize.Default,
    textStyle: TextStyle? = null,
) {
    val minHeight = if (size == AyahButtonSize.Small) 32.dp else 40.dp
    val hPadding = if (size == AyahButtonSize.Small) 12.dp else 16.dp
    val vPadding = if (size == AyahButtonSize.Small) 5.dp else 8.dp
    val fontSize = if (size == AyahButtonSize.Small) 13.sp else 14.sp

    val containerColor: Color
    val contentColor: Color
    val border: BorderStroke?
    when (variant) {
        AyahButtonVariant.Primary -> {
            containerColor = AyahColors.Primary
            contentColor = AyahColors.OnPrimary
            border = null
        }
        AyahButtonVariant.Secondary -> {
            containerColor = AyahColors.Secondary
            contentColor = AyahColors.OnSecondary
            border = null
        }
        AyahButtonVariant.Outline -> {
            containerColor = Color.Transparent
            contentColor = AyahColors.Primary
            border = BorderStroke(1.dp, AyahColors.Primary.copy(alpha = 0.55f))
        }
        AyahButtonVariant.Ghost -> {
            containerColor = Color.Transparent
            contentColor = AyahColors.TextSecondary
            border = null
        }
        AyahButtonVariant.Danger -> {
            containerColor = AyahColors.Error
            contentColor = Color.White
            border = null
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .background(
                color = if (enabled) containerColor else containerColor.copy(alpha = 0.3f),
                shape = AyahShapes.Button,
            )
            .then(if (border != null) Modifier.border(border, AyahShapes.Button) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = hPadding, vertical = vPadding),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = (textStyle ?: AyahTypography.Button).copy(
                fontSize = textStyle?.fontSize ?: fontSize,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            ),
        )
    }
}
