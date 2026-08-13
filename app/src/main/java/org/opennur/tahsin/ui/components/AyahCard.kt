package org.opennur.tahsin.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahShapes

/**
 * Kartu kustom (tanpa Material 3): sudut 14dp, latar Surface, garis hairline
 * tipis (gaya flat modern, tanpa bayangan berat). Opsional klik / klik lama.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AyahCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = AyahShapes.Card
    var cardModifier = modifier
        .background(AyahColors.Surface, shape)
        .border(1.dp, AyahColors.Hairline, shape)

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
