package com.tahsin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.ui.components.DropdownOption
import com.tahsin.app.ui.components.SimpleDropdown
import com.tahsin.app.util.FontStore

/**
 * Layar statistik & riwayat kesalahan: ringkasan global (percobaan, skor,
 * ayat dilatih) + "kata yang sering salah" per surah. Ketuk satu baris kata
 * → langsung buka ayat itu di layar utama untuk latihan.
 */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: StatsViewModel = viewModel(factory = statsViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    // Muat ulang setiap layar dibuka (mungkin ada riwayat baru dari latihan).
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
                    strings.statsTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.statsSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.statsLoading,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
            } else if (state.totalAttempts == 0) {
                // ---- Belum ada riwayat sama sekali ----
                AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.statsNoData,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
            } else {
                // ---- Ringkasan global (2×2) ----
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = strings.statsTotalAttempts,
                        value = "${state.totalAttempts}",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = strings.statsAvgScore,
                        value = "${state.avgScore}%",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = strings.statsBestScore,
                        value = "${state.bestScore}%",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = strings.statsAyahsPracticed,
                        value = "${state.practicedAyahs}",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---- Pilih surah ----
                AyahText(
                    strings.statsSurahSection,
                    style = AyahTypography.Overline.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                SimpleDropdown(
                    selectedLabel = "${state.selectedSurah}. ${state.selectedName}",
                    options = state.surahs.map { s ->
                        DropdownOption("${s.number}. ${s.nameLatin}") {
                            viewModel.selectSurah(s.number)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Kata yang sering salah di surah terpilih ----
                AyahText(
                    "${strings.statsFrequentWords} — ${state.selectedName}",
                    style = AyahTypography.Heading2,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.wordRows.isEmpty()) {
                    AyahCard(modifier = Modifier.fillMaxWidth()) {
                        AyahText(
                            strings.statsEmptySurah,
                            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                        )
                    }
                } else {
                    state.wordRows.forEach { row ->
                        WordErrorCard(
                            row = row,
                            wrongCountLabel = strings.statsWrongCount.format(row.errorCount),
                            ayahLabel = strings.ayahLabel,
                            fontFamily = arabicFamily,
                            onClick = { onOpenAyah(state.selectedSurah, row.ayahNumber) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---- Hapus riwayat ----
                AyahButton(
                    text = strings.statsClear,
                    variant = AyahButtonVariant.Outline,
                    onClick = viewModel::requestClear,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ---- Konfirmasi hapus riwayat ----
        if (state.showClearConfirm) {
            ClearHistoryDialog(
                strings = strings,
                onConfirm = viewModel::confirmClear,
                onCancel = viewModel::cancelClear,
            )
        }
    }
}

/** Kartu kecil ringkasan (label + nilai besar). */
@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AyahCard(modifier = modifier) {
        AyahText(
            value,
            style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
        )
        Spacer(modifier = Modifier.height(2.dp))
        AyahText(
            label,
            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
        )
    }
}

/** Satu baris kata yang sering salah: "Ayat N · <kata> · N× salah/terlewat". */
@Composable
private fun WordErrorCard(
    row: WordErrorRow,
    wrongCountLabel: String,
    ayahLabel: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                "$ayahLabel ${row.ayahNumber}",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(
                row.word,
                style = AyahTypography.Arabic.copy(
                    fontSize = 19.sp,
                    fontFamily = fontFamily,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                wrongCountLabel,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Error,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/** Dialog konfirmasi hapus seluruh riwayat bacaan. */
@Composable
private fun ClearHistoryDialog(
    strings: Strings,
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
            AyahText(strings.statsClearTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                strings.statsClearBody,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(
                    text = strings.statsCancel,
                    variant = AyahButtonVariant.Outline,
                    onClick = onCancel,
                )
                Spacer(modifier = Modifier.width(10.dp))
                AyahButton(
                    text = strings.statsClear,
                    variant = AyahButtonVariant.Danger,
                    onClick = onConfirm,
                )
            }
        }
    }
}
