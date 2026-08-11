package com.ayahofday.app.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ayahofday.app.theme.AyahColors
import com.ayahofday.app.theme.AyahShapes
import com.ayahofday.app.theme.AyahTypography

enum class AyahButtonVariant {
    Primary,
    Secondary,
    Outline,
}

/**
 * Tombol kustom (tanpa Material 3).
 * - Touch target minimal 48dp (accessibility)
 * - Sudut 12dp
 * - Varian: Primary (hijau), Secondary (gold), Outline
 */
@Composable
fun AyahButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AyahButtonVariant = AyahButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val containerColor: Color
    val contentColor: Color
    when (variant) {
        AyahButtonVariant.Primary -> {
            containerColor = AyahColors.Primary
            contentColor = AyahColors.OnPrimary
        }
        AyahButtonVariant.Secondary -> {
            containerColor = AyahColors.Secondary
            contentColor = AyahColors.OnSecondary
        }
        AyahButtonVariant.Outline -> {
            containerColor = Color.Transparent
            contentColor = AyahColors.Primary
        }
    }
    val border = if (variant == AyahButtonVariant.Outline) {
        BorderStroke(1.5.dp, AyahColors.Primary)
    } else {
        null
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(AyahShapes.Button)
            .then(if (border != null) Modifier.border(border) else Modifier)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = AyahTypography.Button.copy(
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            ),
        )
    }
}
