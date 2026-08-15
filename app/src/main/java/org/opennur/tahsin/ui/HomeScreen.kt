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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.learning.DailyLearningPlan
import org.opennur.tahsin.data.learning.LearningGoal
import org.opennur.tahsin.data.learning.LearningTaskType
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
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
data class HomeLearningActions(
    val onOpenTahsin: () -> Unit,
    val onOpenVocab: () -> Unit,
    val onOpenMemorization: () -> Unit,
    val onOpenQuiz: () -> Unit,
    val onOpenAyatQuiz: () -> Unit,
    val onOpenLughoh: () -> Unit,
    val onOpenDreamBig: () -> Unit,
)

data class HomeUtilityActions(
    val onOpenStats: () -> Unit,
    val onOpenBadges: () -> Unit,
    val onOpenCoherence: () -> Unit,
    val onOpenFavorites: () -> Unit,
    val onOpenSettings: () -> Unit,
)

data class HomeActions(
    val learning: HomeLearningActions,
    val utility: HomeUtilityActions,
    val onOpenTask: (LearningTaskType) -> Unit,
)

@Composable
fun HomeScreen(
    actions: HomeActions,
    learningPlan: LearningPlanUiState,
    settings: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(settings.language)
    val gamificationViewModel: GamificationViewModel = viewModel()
    val gamification by gamificationViewModel.state.collectAsStateWithLifecycle()
    // Muat ulang tiap Home masuk komposisi (setelah pop dari fitur lain).
    LaunchedEffect(Unit) { gamificationViewModel.refresh() }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
    // Layar lebar (tablet): konten dibatasi 640dp dan ditengahkan.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .align(Alignment.TopCenter)
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

        if (!learningPlan.loading) {
            TodayPlanCard(
                plan = learningPlan.plan,
                goal = learningPlan.goal,
                dailyMinutes = learningPlan.dailyMinutes,
                strings = strings,
                onOpenTask = actions.onOpenTask,
            )
            Spacer(modifier = Modifier.height(18.dp))
            AyahText(
                strings.todayExplore,
                style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!gamification.isLoading) {
            HomeGamificationCard(gamification, settings.language, strings)
            Spacer(modifier = Modifier.height(16.dp))
        }

        HomeLearningMenu(strings, actions.learning)
        HomeUtilityMenu(strings, actions.utility)

        Spacer(modifier = Modifier.height(28.dp))
        CreditLink(
            text = strings.credit,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
}

@Composable
private fun HomeGamificationCard(
    gamification: GamificationUiState,
    language: org.opennur.tahsin.util.AppLanguage,
    strings: Strings,
) {
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
                gamification.latestBadgeKey?.let { key ->
                    val badge = Achievements.byKey(key)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        AyahText(
                            strings.homeBadgeLabel,
                            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                        )
                        AyahText(
                            "${badge?.emoji.orEmpty()} ${AppStrings.badgeTitle(key, language)} · " +
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
            Spacer(modifier = Modifier.height(10.dp))
            AyahText(
                strings.homeGoalLine.format(gamification.todayXp, gamification.dailyGoalXp),
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(6.dp))
            GoalProgressBar(fraction = gamification.todayXp.toFloat() / gamification.dailyGoalXp)
        }
    }
}

@Composable
private fun HomeLearningMenu(strings: Strings, actions: HomeLearningActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMenuCard(
            text = strings.menuTahsin,
            onClick = actions.onOpenTahsin,
            highlighted = true,
            modifier = Modifier.weight(1f),
        )
        HomeMenuCard(
            text = strings.menuVocab,
            onClick = actions.onOpenVocab,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    HomeMenuCard(
        text = strings.menuMemorization,
        onClick = actions.onOpenMemorization,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMenuCard(
            text = strings.quizTitle,
            onClick = actions.onOpenQuiz,
            modifier = Modifier.weight(1f),
        )
        HomeMenuCard(
            text = strings.menuAyatQuiz,
            onClick = actions.onOpenAyatQuiz,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMenuCard(
            text = strings.menuLughoh,
            onClick = actions.onOpenLughoh,
            modifier = Modifier.weight(1f),
        )
        HomeMenuCard(
            text = strings.menuDreamBig,
            onClick = actions.onOpenDreamBig,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun HomeUtilityMenu(strings: Strings, actions: HomeUtilityActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMenuCard(
            text = strings.menuStats,
            onClick = actions.onOpenStats,
            modifier = Modifier.weight(1f),
        )
        HomeMenuCard(
            text = strings.menuBadges,
            onClick = actions.onOpenBadges,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeMenuCard(
            text = strings.menuCoherence,
            onClick = actions.onOpenCoherence,
            modifier = Modifier.weight(1f),
        )
        HomeMenuCard(
            text = strings.menuFavorites,
            onClick = actions.onOpenFavorites,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    HomeMenuCard(
        text = strings.menuSettings,
        onClick = actions.onOpenSettings,
        highlighted = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TodayPlanCard(
    plan: DailyLearningPlan,
    goal: LearningGoal,
    dailyMinutes: Int,
    strings: Strings,
    onOpenTask: (LearningTaskType) -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(strings.todayTitle, style = AyahTypography.Heading2)
                    AyahText(
                        strings.todaySubtitle.format(dailyMinutes),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                AyahText(
                    strings.todayProgress.format(plan.completedCount, plan.totalCount),
                    style = AyahTypography.Caption.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                goalLabel(goal, strings),
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(8.dp))
            GoalProgressBar(
                fraction = if (plan.totalCount == 0) 0f else {
                    plan.completedCount.toFloat() / plan.totalCount
                },
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (plan.isComplete) {
                AyahText(
                    strings.todayComplete,
                    style = AyahTypography.Body2.copy(
                        color = AyahColors.Success,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            } else {
                plan.tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AyahText(
                            if (task.completed) "✓" else "${task.order + 1}",
                            style = AyahTypography.Body2.copy(
                                color = if (task.completed) AyahColors.Success else AyahColors.Primary,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.width(28.dp),
                        )
                        AyahText(
                            taskLabel(task.type, strings),
                            style = AyahTypography.Body2.copy(
                                color = if (task.completed) AyahColors.TextSecondary else AyahColors.TextPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        AyahButton(
                            text = if (task.completed) strings.todayTaskDone else strings.todayTaskStart,
                            variant = if (task.completed) {
                                AyahButtonVariant.Ghost
                            } else {
                                AyahButtonVariant.Outline
                            },
                            size = AyahButtonSize.Small,
                            enabled = !task.completed,
                            onClick = { onOpenTask(task.type) },
                        )
                    }
                }
            }
        }
    }
}

private fun goalLabel(goal: LearningGoal, strings: Strings): String = when (goal) {
    LearningGoal.RECITATION -> strings.goalRecitation
    LearningGoal.UNDERSTANDING -> strings.goalUnderstanding
    LearningGoal.MEMORIZATION -> strings.goalMemorization
    LearningGoal.ARABIC -> strings.goalArabic
}

private fun taskLabel(type: LearningTaskType, strings: Strings): String = when (type) {
    LearningTaskType.RECITE -> strings.taskRecite
    LearningTaskType.TAJWID -> strings.taskTajwid
    LearningTaskType.VOCABULARY -> strings.taskVocabulary
    LearningTaskType.UNDERSTAND -> strings.taskUnderstand
    LearningTaskType.ARABIC -> strings.taskArabic
    LearningTaskType.MEMORIZATION -> strings.taskMemorization
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
