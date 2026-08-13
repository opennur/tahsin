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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.data.lughoh.LughohEngine
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahText

/**
 * Layar statistik keseluruhan: angka gabungan semua challenge (Tahsin,
 * Dream BIG, Belajar Arab, Kosakata) — total sesi, skor terbaik, total
 * ronde, dan kata dikuasai, plus rincian ringkas per fitur.
 */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: StatsViewModel = viewModel(factory = statsViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)

    // Muat ulang setiap layar dibuka (mungkin ada aktivitas baru).
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

            val isEmpty = !state.isLoading &&
                state.totalSessions == 0 &&
                state.totalRounds == 0 &&
                state.wordsMastered == 0

            when {
                state.isLoading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.statsLoading,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
                isEmpty -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.statsNoData,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
                else -> {
                    // ---- Ringkasan gabungan (2×2) ----
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = strings.statsTotalSessions,
                            value = "${state.totalSessions}",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = strings.statsBestScoreLabel,
                            value = "${state.bestScorePct}%",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = strings.statsTotalRounds,
                            value = "${state.totalRounds}",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = strings.statsWordsMastered,
                            value = "${state.wordsMastered}",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Rincian per fitur ----
                    AyahCard(modifier = Modifier.fillMaxWidth()) {
                        BreakdownLine(strings.statsTahsinLine.format(state.tahsinAttempts))
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownLine(
                            strings.statsDreamBigLine.format(
                                state.dreamBigRounds,
                                state.dreamBigBest,
                                DreamBigGame.QUESTIONS_PER_ROUND,
                            ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownLine(
                            strings.statsLughohLine.format(
                                state.lughohRounds,
                                state.lughohBest,
                                LughohEngine.SESSION_SIZE,
                            ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownLine(strings.statsVocabLine.format(state.wordsMastered))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    AyahCard(modifier = modifier) {
        Column {
            AyahText(
                value,
                style = AyahTypography.Heading1.copy(color = AyahColors.Primary),
            )
            AyahText(
                label,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
        }
    }
}

@Composable
private fun BreakdownLine(text: String) {
    AyahText(
        text,
        style = AyahTypography.Body2.copy(
            color = AyahColors.TextPrimary,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
