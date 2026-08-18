package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.dreambig.DreamBigGame
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.GoalProgressBar
import org.opennur.tahsin.util.Achievements
import org.opennur.tahsin.util.ReadingHistoryEntry
import org.opennur.tahsin.util.RelativeTime
import org.opennur.tahsin.util.OfflineProgressReport
import java.time.LocalDate

/**
 * Layar statistik keseluruhan: angka gabungan semua challenge (Tahsin,
 * Dream BIG, Belajar Arab, Kosakata) — total sesi, skor terbaik, total
 * ronde, dan kata dikuasai, plus rincian ringkas per fitur.
 */
@Composable
@Suppress("LongMethod")
fun StatsScreen(
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit = { _, _ -> },
    onOpenPetaKhatam: () -> Unit = {},
    onShareReport: (OfflineProgressReport) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: StatsViewModel = viewModel()
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
            Spacer(modifier = Modifier.height(10.dp))
            AyahButton(
                text = strings.statsShareReport,
                variant = AyahButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onShareReport(viewModel.progressReport()) },
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

                    // ---- Ringkasan gamification (XP/level/streak/badge) ----
                    AyahCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                AyahText(
                                    strings.homeLevelLine.format(state.level, state.xp),
                                    style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                                )
                                AyahText(
                                    strings.homeStreakLine.format(state.streak),
                                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                AyahText(
                                    strings.homeGoalLine.format(state.todayXp, state.dailyGoalXp),
                                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                GoalProgressBar(
                                    fraction = state.todayXp.toFloat() / state.dailyGoalXp,
                                )
                            }
                            val latestKey = state.latestBadgeKey
                            if (latestKey != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    AyahText(
                                        strings.badgesCount.format(state.badgesCount, Achievements.ALL.size),
                                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                                    )
                                    val badge = Achievements.byKey(latestKey)
                                    AyahText(
                                        "${badge?.emoji.orEmpty()} ${AppStrings.badgeTitle(latestKey, state.language)} · " +
                                            strings.badgesTierLabel.format(state.latestBadgeTier),
                                        style = AyahTypography.Body2.copy(
                                            color = AyahColors.TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.End,
                                        ),
                                    )
                                }
                            } else {
                                AyahText(
                                    strings.badgesCount.format(state.badgesCount, Achievements.ALL.size),
                                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    AyahCard(modifier = Modifier.fillMaxWidth()) {
                        AyahText(
                            strings.statsPlanLine.format(
                                state.dailyPlanCompleted,
                                state.dailyPlanTotal,
                            ),
                            style = AyahTypography.Body2.copy(
                                color = AyahColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    StatsReadingProgressSection(
                        state = state,
                        strings = strings,
                        onOpenAyah = onOpenAyah,
                    )

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
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownLine(
                            strings.statsNahwuLine.format(
                                state.nahwuRounds,
                                state.nahwuBest,
                                org.opennur.tahsin.data.nahwu.NahwuEngine.SESSION_SIZE,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Riwayat baca (ayat terakhir yang dibuka) ----
                    AyahText(
                        strings.statsHistoryTitle,
                        style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.history.isEmpty()) {
                        AyahCard(modifier = Modifier.fillMaxWidth()) {
                            AyahText(
                                strings.statsHistoryEmpty,
                                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                            )
                        }
                    } else {
                        AyahCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                state.history.take(10).forEach { entry ->
                                    val name = state.surahNames[entry.surah] ?: "Surah ${entry.surah}"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenAyah(entry.surah, entry.ayah) }
                                            .padding(vertical = 8.dp),
                                    ) {
                                        AyahText(
                                            "$name · ${entry.ayah} · " +
                                                RelativeTime.format(
                                                    entry.timestamp,
                                                    System.currentTimeMillis(),
                                                    state.language,
                                                ),
                                            style = AyahTypography.Body2.copy(color = AyahColors.TextPrimary),
                                            modifier = Modifier.weight(1f),
                                        )
                                        AyahText(
                                            "›",
                                            style = AyahTypography.Body2.copy(color = AyahColors.Primary),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Peta Khatam ----
            AyahButton(
                text = strings.petaTitle,
                variant = AyahButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenPetaKhatam,
            )

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
private fun StatsReadingProgressSection(
    state: StatsState,
    strings: Strings,
    onOpenAyah: (Int, Int) -> Unit,
) {
    val progress = state.readingProgress
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(strings.statsReadingTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                strings.statsReadingSummary.format(
                    progress.practicedAyahs,
                    progress.totalAyahs,
                    progress.practicedPercent,
                    progress.goodJuz,
                ),
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                strings.homeReadingPages.format(
                    progress.goodPages,
                    progress.goodPages + progress.reviewPages + progress.untouchedPages,
                ),
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )

            Spacer(modifier = Modifier.height(12.dp))
            AyahText(strings.statsDueTitle, style = AyahTypography.Heading2)
            if (state.nextReviews.isEmpty()) {
                AyahText(
                    strings.statsNoDue,
                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                )
            } else {
                val today = LocalDate.now().toEpochDay()
                state.nextReviews.forEach { review ->
                    val name = state.surahNames[review.surahNumber] ?: "#${review.surahNumber}"
                    AyahText(
                        "$name · ${review.ayahNumber} · " +
                            strings.tahsinResultNextReview.format(
                                (review.reviewDueDay - today).coerceAtLeast(0L),
                            ),
                        style = AyahTypography.Body2.copy(color = AyahColors.Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAyah(review.surahNumber, review.ayahNumber) }
                            .padding(vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            AyahText(strings.statsJuzProgress, style = AyahTypography.Heading2)
            progress.juz.forEach { juz ->
                ProgressLine(
                    label = strings.statsJuzRow.format(
                        juz.juz,
                        juz.practicedAyahs,
                        juz.totalAyahs,
                        juz.goodAyahs,
                    ),
                    fraction = if (juz.totalAyahs == 0) 0f else {
                        juz.practicedAyahs.toFloat() / juz.totalAyahs
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            AyahText(strings.statsSurahProgress, style = AyahTypography.Heading2)
            progress.surahs.filter { it.practicedAyahs > 0 || it.dueAyahs > 0 }.forEach { surah ->
                ProgressLine(
                    label = strings.statsSurahRow.format(
                        surah.name,
                        surah.practicedAyahs,
                        surah.totalAyahs,
                        surah.goodAyahs,
                    ),
                    fraction = if (surah.totalAyahs == 0) 0f else {
                        surah.practicedAyahs.toFloat() / surah.totalAyahs
                    },
                )
            }
        }
    }
}

@Composable
private fun ProgressLine(label: String, fraction: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        AyahText(label, style = AyahTypography.Caption)
        GoalProgressBar(fraction = fraction)
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
