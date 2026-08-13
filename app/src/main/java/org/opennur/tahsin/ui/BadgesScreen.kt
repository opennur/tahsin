package org.opennur.tahsin.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.GoalProgressBar
import org.opennur.tahsin.util.BadgeProgress

/**
 * Layar Penghargaan: ringkasan level/XP + daftar semua badge
 * (diraih berwarna, terkunci redup) dengan judul & deskripsi terjemahan.
 */
@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: BadgesViewModel = viewModel(factory = badgesViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)

    // Evaluasi ulang badge setiap layar dibuka.
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
                    strings.badgesTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.badgesSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                    AyahText(
                        strings.badgesLoading,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                    )
                }
                else -> {
                    // ---- Ringkasan level & XP ----
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "${strings.levelLabel} ${state.level}",
                            value = "${state.xp} XP",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = strings.badgesCount.format(state.earnedCount, state.totalCount),
                            value = "${state.earnedCount}/${state.totalCount}",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---- Daftar badge (progres tier tak terbatas) ----
                    state.badges.forEach { badge ->
                        BadgeRow(
                            emoji = badge.def.emoji,
                            title = AppStrings.badgeTitle(badge.def.key, state.language),
                            description = AppStrings.badgeDesc(badge.def.key, state.language),
                            progress = badge.progress,
                            tierLabel = strings.badgesTierLabel,
                            lockedLabel = strings.badgesLocked,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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

/**
 * Satu baris badge: emoji + judul + deskripsi + TIER + progress menuju tier
 * berikutnya (target tak terbatas). Terkunci (tier 0) diredupkan.
 */
@Composable
private fun BadgeRow(
    emoji: String,
    title: String,
    description: String,
    progress: BadgeProgress,
    tierLabel: String,
    lockedLabel: String,
) {
    val unlocked = progress.isUnlocked
    val emojiAlpha = if (unlocked) 1f else 0.35f
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahText(
                    emoji,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.width(48.dp).graphicsLayer { alpha = emojiAlpha },
                )
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(
                        title,
                        style = AyahTypography.Body2.copy(
                            color = if (unlocked) AyahColors.TextPrimary else AyahColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    AyahText(
                        description,
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AyahText(
                    if (unlocked) tierLabel.format(progress.currentTier) else lockedLabel,
                    style = AyahTypography.Caption.copy(
                        color = if (unlocked) AyahColors.Success else AyahColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            // Progress menuju tier berikutnya — selalu ada target baru.
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "${progress.metricValue}/${progress.nextThreshold}",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(4.dp))
            GoalProgressBar(fraction = progress.fraction)
        }
    }
}
