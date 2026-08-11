package com.ayahofday.app.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Pengganti `Text` Material 3 — dibangun di atas `BasicText` (foundation).
 * `style` diwajibkan agar warna teks selalu eksplisit dan konsisten.
 */
@Composable
fun AyahText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (color != null) style.copy(color = color) else style,
        maxLines = maxLines,
        overflow = overflow,
    )
}
