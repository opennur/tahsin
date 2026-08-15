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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.opennur.tahsin.data.learning.LearningGoal
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage

@Composable
fun OnboardingScreen(
    language: AppLanguage,
    initialGoal: LearningGoal = LearningGoal.RECITATION,
    initialMinutes: Int = 15,
    onSetLanguage: (AppLanguage) -> Unit,
    onComplete: (LearningGoal, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(language)
    var goalKey by rememberSaveable(initialGoal.key) { mutableStateOf(initialGoal.key) }
    var minutes by rememberSaveable(initialMinutes) { mutableStateOf(initialMinutes) }
    val goal = LearningGoal.fromKey(goalKey)

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OnboardingIntro(strings)
            Spacer(modifier = Modifier.height(18.dp))
            OnboardingGoalSection(
                strings = strings,
                goal = goal,
                onSelect = { goalKey = it.key },
            )
            Spacer(modifier = Modifier.height(18.dp))
            OnboardingTimeSection(strings, minutes) { minutes = it }
            Spacer(modifier = Modifier.height(8.dp))
            OnboardingLanguageSection(strings, language, onSetLanguage)
            Spacer(modifier = Modifier.height(24.dp))
            AyahButton(
                text = strings.onboardingStart,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onComplete(goal, minutes) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingIntro(strings: Strings) {
    AyahText(
        strings.onboardingTitle,
        style = AyahTypography.Heading1.copy(textAlign = TextAlign.Center),
    )
    Spacer(modifier = Modifier.height(8.dp))
    AyahText(
        strings.onboardingSubtitle,
        style = AyahTypography.Body2.copy(
            color = AyahColors.TextSecondary,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OnboardingGoalSection(
    strings: Strings,
    goal: LearningGoal,
    onSelect: (LearningGoal) -> Unit,
) {
    AyahText(
        strings.onboardingGoalQuestion,
        style = AyahTypography.Heading2,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    GoalOption(strings.goalRecitation, goal == LearningGoal.RECITATION) {
        onSelect(LearningGoal.RECITATION)
    }
    GoalOption(strings.goalUnderstanding, goal == LearningGoal.UNDERSTANDING) {
        onSelect(LearningGoal.UNDERSTANDING)
    }
    GoalOption(strings.goalMemorization, goal == LearningGoal.MEMORIZATION) {
        onSelect(LearningGoal.MEMORIZATION)
    }
    GoalOption(strings.goalArabic, goal == LearningGoal.ARABIC) {
        onSelect(LearningGoal.ARABIC)
    }
}

@Composable
private fun OnboardingTimeSection(
    strings: Strings,
    minutes: Int,
    onSelect: (Int) -> Unit,
) {
    AyahText(
        strings.onboardingTimeQuestion,
        style = AyahTypography.Heading2,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(5, 15, 30, 60).forEach { value ->
            AyahButton(
                text = strings.onboardingMinutes.format(value),
                variant = if (minutes == value) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun OnboardingLanguageSection(
    strings: Strings,
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(
                strings.settingLanguage,
                style = AyahTypography.Body2.copy(
                    color = AyahColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageButton(
                    label = strings.languageNameId,
                    selected = language == AppLanguage.ID,
                    modifier = Modifier.weight(1f),
                ) {
                    onSetLanguage(AppLanguage.ID)
                }
                LanguageButton(
                    label = strings.languageNameEn,
                    selected = language == AppLanguage.EN,
                    modifier = Modifier.weight(1f),
                ) {
                    onSetLanguage(AppLanguage.EN)
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AyahButton(
        text = label,
        variant = if (selected) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun GoalOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AyahButton(
        text = label,
        variant = if (selected) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        onClick = onClick,
    )
}
