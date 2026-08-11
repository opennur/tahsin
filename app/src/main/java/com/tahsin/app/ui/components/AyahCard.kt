package com.tahsin.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahShapes

/**
 * Kartu kustom (tanpa Material 3): sudut 16dp, latar Surface, elevasi halus.
 * Opsional `onClick` / `onLongClick` untuk membuat kartu interaktif.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = AyahShapes.Card
    var cardModifier = modifier
        .shadow(elevation = 2.dp, shape = shape)
        .clip(shape)
        .background(AyahColors.Surface)

    when {
        onClick != null && onLongClick != null ->
            cardModifier = cardModifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null ->
            cardModifier = cardModifier.clickable(onClick = onClick)
        onLongClick != null ->
            cardModifier = cardModifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
    }

    Box(modifier = cardModifier) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
