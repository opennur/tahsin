package com.tahsin.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import java.util.Locale

/**
 * Layar manajemen audio terunduh: lihat surah mana saja yang sudah punya
 * audio (per ayat + per kata), ukurannya, dan hapus per surah (dengan
 * konfirmasi).
 */
@Composable
fun AudioManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: AudioManagerViewModel = viewModel(factory = audioManagerViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)

    // Refresh setiap kali layar dibuka (mungkin ada unduhan baru dari TahsinScreen).
    LaunchedEffect(Unit) { viewModel.refresh() }

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
                strings.audioManagerTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
            if (state.items.isNotEmpty()) {
                AyahButton(
                    text = strings.audioDeleteAll,
                    variant = AyahButtonVariant.Outline,
                    onClick = viewModel::requestDeleteAll,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.audioManagerSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.items.isEmpty()) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    strings.audioNoDownload,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            AyahText(
                "Total: ${state.totalDownloaded} file • ${formatSize(state.totalSizeBytes)}",
                style = AyahTypography.Caption,
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.items.forEach { item ->
                AudioItemCard(
                    item = item,
                    strings = strings,
                    onDelete = { viewModel.requestDelete(item.number) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ---- Dialog konfirmasi hapus per surah ----
    val pending = state.pendingDelete
    if (pending != null) {
        val item = state.items.find { it.number == pending }
        DeleteConfirmDialog(
            strings = strings,
            title = "${strings.audioDeleteTitle} ${item?.let { "${it.number}. ${it.nameLatin}" } ?: "$pending"}",
            message = strings.audioDeleteBody + (item?.let { " (${formatSize(it.sizeBytes)})" } ?: ""),
            onConfirm = viewModel::confirmDelete,
            onCancel = viewModel::cancelDelete,
        )
    }

    // ---- Dialog konfirmasi hapus semua ----
    if (state.pendingDeleteAll) {
        DeleteConfirmDialog(
            strings = strings,
            title = strings.audioDeleteAllTitle,
            message = strings.audioDeleteAllBody + " (${formatSize(state.totalSizeBytes)})",
            onConfirm = viewModel::confirmDeleteAll,
            onCancel = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun AudioItemCard(
    item: AudioManagerItem,
    strings: Strings,
    onDelete: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    "${item.number}. ${item.nameLatin}",
                    style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                )
                val missingNote = if (item.missingWords > 0 || item.missingAyahs > 0) {
                    " • " + strings.audioMissingNote.format(item.missingWords + item.missingAyahs)
                } else ""
                AyahText(
                    "${strings.audioAyat}: ${item.ayahFiles}/${item.ayahCount} • ${strings.audioKata}: ${item.wordFiles}/${item.totalWords ?: "?"} • ${formatSize(item.sizeBytes)}$missingNote",
                    style = AyahTypography.Caption,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AyahText(
                    if (item.isComplete) strings.audioStatusComplete else strings.audioStatusIncomplete,
                    style = AyahTypography.Caption.copy(
                        color = if (item.isComplete) AyahColors.Success else AyahColors.Error,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            AyahButton(
                text = strings.audioDelete,
                variant = AyahButtonVariant.Outline,
                onClick = onDelete,
            )
        }
    }
}

/** Dialog konfirmasi hapus audio. */
@Composable
private fun DeleteConfirmDialog(
    strings: Strings,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
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
            AyahText(title, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                message,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(text = strings.audioCancel, variant = AyahButtonVariant.Outline, onClick = onCancel)
                Spacer(modifier = Modifier.width(12.dp))
                AyahButton(text = strings.audioDelete, variant = AyahButtonVariant.Primary, onClick = onConfirm)
            }
        }
    }
}

/** Format ukuran: MB kalau >= 1 MB, selain itu KB. */
private fun formatSize(bytes: Long): String =
    if (bytes >= 1_048_576L) {
        String.format(Locale.US, "%.2f MB", bytes / 1_048_576.0)
    } else {
        String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
