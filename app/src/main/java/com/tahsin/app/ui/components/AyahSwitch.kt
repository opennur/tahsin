package com.tahsin.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tahsin.app.theme.AyahColors

/**
 * Saklar kustom (tanpa Material 3) — track + thumb dengan animasi halus.
 * Dipakai di drawer pengaturan untuk toggle yang ringkas (hemat ruang
 * vertikal vs tombol "Nyala/Mati" selebar panel).
 *
 * Saat [interactive] = false, saklar hanya tampilan (tanpa klik & tanpa
 * node aksesibilitas sendiri) — berguna bila baris di sekitarnya yang
 * menjadi satu-satunya target sentuh, supaya TalkBack tidak mendengar
 * dua saklar untuk satu setelan.
 */
@Composable
fun AyahSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val width = 46.dp
    val height = 26.dp
    val thumbSize = 20.dp
    val padding = 3.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - padding else padding,
        label = "switchThumb",
    )

    val tapModifier = if (interactive) {
        Modifier.clickable(role = Role.Switch) { onCheckedChange(!checked) }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) AyahColors.Primary else AyahColors.SurfaceVariant)
            .then(tapModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(thumbSize)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}
