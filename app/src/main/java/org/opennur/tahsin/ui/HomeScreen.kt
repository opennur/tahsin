package org.opennur.tahsin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahShapes
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.CreditLink
import org.opennur.tahsin.ui.components.GoalProgressBar
import org.opennur.tahsin.util.Achievements

/**
 * Portal layar utama: grid kartu menu untuk semua fitur.
 *
 * Tiap menu membuka layarnya sendiri (di-push di atas Home di MainActivity) —
 * tanpa drawer. Bahasa & mode gelap mengikuti [SettingsUiState] bersama.
 */
@Composable
fun HomeScreen(
    onOpenTahsin: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDreamBig: () -> Unit,
    onOpenLughoh: () -> Unit,
    onOpenAyatQuiz: () -> Unit,
    onOpenBadges: () -> Unit,
    onOpenCoherence: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    settings: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(settings.language)
    val context = LocalContext.current
    val gamificationViewModel: GamificationViewModel = viewModel(
        factory = gamificationViewModelFactory(context),
    )
    val gamification by gamificationViewModel.state.collectAsStateWithLifecycle()
    // Muat ulang tiap Home masuk komposisi (setelah pop dari fitur lain).
    LaunchedEffect(Unit) { gamificationViewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(strings.appTitle, style = AyahTypography.Heading1)
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.homeSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!gamification.isLoading) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            AyahText(
                                strings.homeLevelLine.format(gamification.level, gamification.xp),
                                style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
                            )
                            AyahText(
                                strings.homeStreakLine.format(gamification.streak),
                                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                            )
                        }
                        val latestKey = gamification.latestBadgeKey
                        if (latestKey != null) {
                            val badge = Achievements.byKey(latestKey)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                AyahText(
                                    strings.homeBadgeLabel,
                                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                                )
                                AyahText(
                                    "${badge?.emoji.orEmpty()} ${AppStrings.badgeTitle(latestKey, settings.language)} · " +
                                        strings.badgesTierLabel.format(gamification.latestBadgeTier),
                                    style = AyahTypography.Body2.copy(
                                        color = AyahColors.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                    ),
                                )
                            }
                        }
                    }
                    // Target harian XP (direset otomatis tiap hari).
                    Spacer(modifier = Modifier.height(10.dp))
                    AyahText(
                        strings.homeGoalLine.format(gamification.todayXp, gamification.dailyGoalXp),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GoalProgressBar(
                        fraction = gamification.todayXp.toFloat() / gamification.dailyGoalXp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Baris 1: Tahsin (utama, disorot) + Kosakata
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.menuTahsin,
                onClick = onOpenTahsin,
                highlighted = true,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuVocab,
                onClick = onOpenVocab,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 2: Kuis Tajwid + Kuis Ayat
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.quizTitle,
                onClick = onOpenQuiz,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuAyatQuiz,
                onClick = onOpenAyatQuiz,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 3: Belajar Arab + Dream BIG
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.menuLughoh,
                onClick = onOpenLughoh,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuDreamBig,
                onClick = onOpenDreamBig,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 4: Statistik + Penghargaan
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.menuStats,
                onClick = onOpenStats,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuBadges,
                onClick = onOpenBadges,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 5: Studi Coherence + Ayat Favorit (di atas Pengaturan)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMenuCard(
                text = strings.menuCoherence,
                onClick = onOpenCoherence,
                modifier = Modifier.weight(1f),
            )
            HomeMenuCard(
                text = strings.menuFavorites,
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Baris 6: Pengaturan (penuh, disorot)
        HomeMenuCard(
            text = strings.menuSettings,
            onClick = onOpenSettings,
            highlighted = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(28.dp))
        CreditLink(
            text = strings.credit,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Kartu menu portal: label di tengah, latar sesuai varian. */
@Composable
private fun HomeMenuCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val container = if (highlighted) AyahColors.PrimarySoft else AyahColors.SurfaceVariant
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AyahText(
            text,
            style = AyahTypography.Body2.copy(
                color = AyahColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
