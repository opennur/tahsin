package com.tahsin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahText

/**
 * Layar manajemen audio terunduh: lihat surah mana saja yang sudah punya
 * audio (per ayat + per kata), seberapa lengkap, dan hapus per surah.
 */
@Composable
fun AudioManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: AudioManagerViewModel = viewModel(factory = audioManagerViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Refresh setiap kali layar dibuka (mungkin ada unduhan baru dari TahsinScreen).
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                "Audio Terunduh",
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
            if (state.items.isNotEmpty()) {
                AyahButton(
                    text = "🗑 Semua",
                    variant = AyahButtonVariant.Outline,
                    onClick = viewModel::deleteAll,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            "Kelola audio yang sudah diunduh per surah.",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    "Belum ada audio terunduh. Buka sebuah surah lalu tekan " +
                        "▶ Dengar Ayat untuk mengunduhnya.",
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            AyahText(
                "Total: ${state.totalDownloaded} file",
                style = AyahTypography.Caption,
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.items.forEach { item ->
                AudioItemCard(
                    item = item,
                    onDelete = { viewModel.deleteSurah(item.number) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AudioItemCard(
    item: AudioManagerItem,
    onDelete: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    "${item.number}. ${item.nameLatin}",
                    style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                )
                AyahText(
                    "Ayat: ${item.ayahFiles}/${item.ayahCount} • Kata: ${item.wordFiles}/${item.totalWords ?: "?"}",
                    style = AyahTypography.Caption,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AyahText(
                    if (item.isComplete) "✓ Lengkap" else "Belum lengkap",
                    style = AyahTypography.Caption.copy(
                        color = if (item.isComplete) AyahColors.Success else AyahColors.Error,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            AyahButton(
                text = "🗑 Hapus",
                variant = AyahButtonVariant.Outline,
                onClick = onDelete,
            )
        }
    }
}
