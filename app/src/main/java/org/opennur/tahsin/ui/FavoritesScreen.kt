package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText

/**
 * Layar Ayat Favorit: daftar ayat yang ditandai ★ di layar Tahsin.
 * Ketuk kartu → buka ayat itu di mushaf (OpenTarget via MainActivity);
 * tombol ★ di kartu menghapus favorit langsung.
 */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: FavoritesViewModel = viewModel(factory = favoritesViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)

    // Muat ulang tiap layar dibuka (VM Activity-scoped: bookmark & bahasa
    // bisa berubah sejak terakhir dibuka).
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---- Header ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                AyahText(
                    strings.favoritesTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.favoritesSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.favoritesLoading,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
                state.items.isEmpty() -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.favoritesEmpty,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
                else -> state.items.forEach { item ->
                    FavoriteRow(
                        item = item,
                        openHint = strings.favoritesOpenHint,
                        onClick = { onOpenAyah(item.surah, item.ayah) },
                        onRemove = { viewModel.remove(item.surah, item.ayah) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Satu kartu ayat favorit: nama surah + nomor, teks Arab, terjemahan, ★ hapus. */
@Composable
private fun FavoriteRow(
    item: FavoriteAyahUi,
    openHint: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    AyahCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(
                        "${item.surahName} · ${item.ayah}",
                        style = AyahTypography.Caption.copy(
                            color = AyahColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    AyahText(
                        openHint,
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                AyahText(
                    "★",
                    style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                    modifier = Modifier
                        .clickable(onClick = onRemove)
                        .padding(8.dp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Teks Arab: font sistem (Noto Naskh) cukup untuk tampilan daftar.
            AyahText(
                item.arabic,
                style = AyahTypography.Arabic.copy(fontSize = 22.sp, textAlign = TextAlign.Start),
            )
            if (item.translation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AyahText(
                    item.translation,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        }
    }
}
