package org.opennur.tahsin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.Strings

/**
 * Dialog unduhan audio — dirender di tingkat MainActivity (global) karena
 * unduhan bisa dimulai dari layar mana pun (Tahsin saat Dengar, atau
 * Pengaturan saat Unduh Semua).
 */

/** Popup keterangan singkat saat unduhan audio dimulai. */
@Composable
fun DownloadNoticeDialog(strings: Strings, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText(strings.downloadNoticeTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                strings.downloadNoticeBody,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(text = strings.gotIt, variant = AyahButtonVariant.Primary, onClick = onDismiss)
            }
        }
    }
}

/** Prompt (sekali) untuk mengizinkan unduhan berjalan di latar belakang. */
@Composable
fun BackgroundPromptDialog(strings: Strings, onSetBackgroundAllowed: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText(strings.bgPromptTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                strings.bgPromptBody,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column {
                AyahButton(
                    text = strings.bgAllow,
                    variant = AyahButtonVariant.Primary,
                    onClick = { onSetBackgroundAllowed(true) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                AyahButton(
                    text = strings.bgDeny,
                    variant = AyahButtonVariant.Outline,
                    onClick = { onSetBackgroundAllowed(false) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
