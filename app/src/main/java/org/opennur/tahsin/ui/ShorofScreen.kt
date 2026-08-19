@file:Suppress("MaxLineLength")

package org.opennur.tahsin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.shorof.ShorofChoiceExercise
import org.opennur.tahsin.data.shorof.ShorofConjugation
import org.opennur.tahsin.data.shorof.ShorofConjugationExercise
import org.opennur.tahsin.data.shorof.ShorofExercise
import org.opennur.tahsin.data.shorof.ShorofLesson
import org.opennur.tahsin.data.shorof.ShorofPattern
import org.opennur.tahsin.data.shorof.ShorofRule
import org.opennur.tahsin.theme.ArabicFont
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahLoadingView
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.FontStore

@Composable
fun ShorofScreen(
    onBack: () -> Unit,
    onGuidedComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: ShorofViewModel = viewModel()
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    BackHandler(enabled = state.mode != ShorofMode.HOME) { viewModel.backToHome() }
    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.shorofLoading)
            state.mode == ShorofMode.HOME -> ShorofHome(
                state = state,
                strings = strings,
                onBack = onBack,
                onStart = viewModel::startExercises,
                onOpenLesson = viewModel::openLesson,
            )
            state.mode == ShorofMode.LESSON -> ShorofLessonView(
                state = state,
                strings = strings,
                arabicFamily = arabicFamily,
                onBack = viewModel::backToHome,
            )
            else -> ShorofExercises(
                state = state,
                strings = strings,
                arabicFamily = arabicFamily,
                onBack = viewModel::backToHome,
                onAnswer = viewModel::answerChoice,
                onNext = viewModel::next,
                onRestart = viewModel::restart,
                onGuidedComplete = onGuidedComplete,
            )
        }
    }
}

private fun localized(language: AppLanguage, id: String, en: String): String =
    if (language == AppLanguage.EN && en.isNotBlank()) en else id

@Composable
private fun ShorofHome(
    state: ShorofUiState,
    strings: Strings,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenLesson: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ShorofHeader(strings.menuShorof, onBack)
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(strings.shorofSubtitle, style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(16.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(strings.shorofStart, style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
                    AyahText(strings.shorofBestScore.format(state.stats.bestScore, 8), style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary))
                    AyahText(strings.shorofSessionsPlayed.format(state.stats.sessionsPlayed), style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary))
                }
                AyahButton(text = "▶", variant = AyahButtonVariant.Outline, onClick = onStart)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AyahText(strings.shorofMaterialTitle, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(6.dp))
        if (state.levels.isEmpty()) {
            AyahText(strings.shorofEmpty, style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary))
        } else {
            state.levels.forEach { level ->
                AyahCard {
                    AyahText(localized(state.language, level.titleId, level.titleEn), style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
                    AyahText(level.titleAr, style = AyahTypography.ArabicWord.copy(fontSize = 15.sp, color = AyahColors.TextSecondary))
                    Spacer(modifier = Modifier.height(8.dp))
                    level.lessons.forEach { lesson ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AyahColors.SurfaceVariant)
                                .clickable { onOpenLesson(lesson.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AyahText(localized(state.language, lesson.titleId, lesson.titleEn), style = AyahTypography.Body2.copy(fontWeight = FontWeight.Medium))
                                AyahText(lesson.titleAr, style = AyahTypography.ArabicWord.copy(fontSize = 13.sp, color = AyahColors.TextSecondary))
                            }
                            if (lesson.completed) AyahText("✓", style = AyahTypography.Body1.copy(color = AyahColors.Success))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ShorofLessonView(
    state: ShorofUiState,
    strings: Strings,
    arabicFamily: FontFamily,
    onBack: () -> Unit,
) {
    val lesson = state.lesson ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ShorofHeader(localized(state.language, lesson.titleId, lesson.titleEn), onBack, lesson.titleAr)
        Spacer(modifier = Modifier.height(16.dp))
        AyahCard {
            AyahText(strings.shorofLessonIntro, style = AyahTypography.Body2.copy(color = AyahColors.Primary))
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(localized(state.language, lesson.introId, lesson.introEn), style = AyahTypography.Body1)
        }
        Spacer(modifier = Modifier.height(16.dp))
        lesson.rules.forEach { rule ->
            ShorofRuleCard(rule, state.language, arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (lesson.patterns.isNotEmpty()) {
            AyahText(strings.shorofPatternTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(6.dp))
            lesson.patterns.forEach { pattern ->
                ShorofPatternCard(pattern, state.language, arabicFamily)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (lesson.conjugations.isNotEmpty()) {
            AyahText(strings.shorofConjugationTitle, style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(6.dp))
            ShorofConjugationTable(lesson.conjugations, arabicFamily)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ShorofRuleCard(rule: ShorofRule, language: AppLanguage, arabicFamily: FontFamily) {
    AyahCard {
        AyahText(localized(language, rule.titleId, rule.titleEn), style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(localized(language, rule.explanationId, rule.explanationEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(rule.exampleAr, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 21.sp, color = AyahColors.Primary))
        AyahText(rule.exampleLatin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
        AyahText(localized(language, rule.exampleId, rule.exampleEn), style = AyahTypography.Body2)
    }
}

@Composable
private fun ShorofPatternCard(pattern: ShorofPattern, language: AppLanguage, arabicFamily: FontFamily) {
    AyahCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(pattern.root, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 20.sp, color = AyahColors.Primary))
                AyahText(pattern.rootLatin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
            }
            Column(horizontalAlignment = Alignment.End) {
                AyahText(pattern.wazan, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 20.sp, color = AyahColors.TextPrimary))
                AyahText(pattern.wazanLatin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(localized(language, pattern.formId, pattern.formEn), style = AyahTypography.Body2.copy(fontWeight = FontWeight.SemiBold))
        AyahText(localized(language, pattern.meaningId, pattern.meaningEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(pattern.exampleAr, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 18.sp, color = AyahColors.Primary))
        AyahText(pattern.exampleLatin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
    }
}

@Composable
private fun ShorofConjugationTable(rows: List<ShorofConjugation>, arabicFamily: FontFamily) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Column {
            ShorofTableRow("الضَّمِيرُ", "Past", "Present", "Command", arabicFamily, header = true)
            rows.forEach { row ->
                ShorofTableRow(row.pronounAr, row.past, row.present, row.imperative, arabicFamily)
            }
        }
    }
}

@Composable
private fun ShorofTableRow(
    pronoun: String,
    past: String,
    present: String,
    imperative: String,
    arabicFamily: FontFamily,
    header: Boolean = false,
) {
    Row(
        modifier = Modifier
            .background(if (header) AyahColors.PrimarySoft else AyahColors.SurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        TableCell(pronoun, arabicFamily, header)
        TableCell(past, arabicFamily, header)
        TableCell(present, arabicFamily, header)
        TableCell(imperative, arabicFamily, header)
    }
}

@Composable
private fun TableCell(value: String, arabicFamily: FontFamily, header: Boolean) {
    Box(modifier = Modifier.widthIn(min = 105.dp).width(105.dp).padding(horizontal = 4.dp)) {
        AyahText(
            value,
            style = if (header) AyahTypography.Caption.copy(fontWeight = FontWeight.Bold) else {
                AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 17.sp)
            },
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun ShorofExercises(
    state: ShorofUiState,
    strings: Strings,
    arabicFamily: FontFamily,
    onBack: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onGuidedComplete: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ShorofHeader(strings.shorofExercisesTitle, onBack)
        if (state.exercisesDone) {
            ShorofResult(
                state = state,
                strings = strings,
                onRestart = onRestart,
                onBack = onGuidedComplete ?: onBack,
                backLabel = if (onGuidedComplete == null) strings.shorofBackHome else strings.guidedReturnHome,
            )
            return
        }
        val exercise = state.exercise
        val optionsPair = exercise?.let { ex ->
            when (ex) {
                is ShorofChoiceExercise -> ex.optionsId to (ex.optionsEn to ex.answerIndex)
                is ShorofConjugationExercise -> ex.optionsId to (ex.optionsEn to ex.answerIndex)
                else -> null
            }
        }
        if (optionsPair == null) {
            AyahText(strings.shorofEmpty, style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary))
            return
        }
        val (optionsId, optionsEnAndAnswer) = optionsPair
        val (optionsEn, answerIndex) = optionsEnAndAnswer
        Spacer(modifier = Modifier.height(12.dp))
        AyahText(strings.shorofProgressLabel.format(state.exerciseIndex + 1, state.total), style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard {
            AyahText(localized(state.language, exercise.promptId, exercise.promptEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(exercise.promptAr, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 22.sp, color = AyahColors.TextPrimary))
            AyahText(exercise.promptLatin, style = AyahTypography.Body2.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
            Spacer(modifier = Modifier.height(12.dp))
            val options = if (state.language == AppLanguage.EN) optionsEn else optionsId
            options.forEachIndexed { index, option ->
                ShorofOption(option, state.selected != null, index == answerIndex, index == state.selected) { onAnswer(index) }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (state.correct != null) {
                AyahText(
                    if (state.correct == true) strings.shorofCorrect else strings.shorofWrong,
                    style = AyahTypography.Body1.copy(
                        color = if (state.correct == true) AyahColors.Success else AyahColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                AyahButton(text = strings.shorofNext, onClick = onNext, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ShorofOption(text: String, answered: Boolean, correct: Boolean, selected: Boolean, onClick: () -> Unit) {
    val container = when {
        answered && correct -> AyahColors.Success
        answered && selected -> AyahColors.Error
        else -> AyahColors.SurfaceVariant
    }
    val color = if (answered && (correct || selected)) Color.White else AyahColors.TextPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(enabled = !answered, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        AyahText(text, style = AyahTypography.Body1.copy(color = color, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun ShorofResult(
    state: ShorofUiState,
    strings: Strings,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    backLabel: String,
) {
    Spacer(modifier = Modifier.height(24.dp))
    AyahCard {
        AyahText(strings.shorofResultTitle, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(strings.shorofScore.format(state.score, state.total), style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(strings.shorofBestScore.format(state.stats.bestScore, state.total), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(14.dp))
        AyahButton(text = strings.shorofRestart, onClick = onRestart, variant = AyahButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(text = backLabel, onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ShorofHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AyahText(title, style = AyahTypography.Heading1)
            subtitle?.let { AyahText(it, style = AyahTypography.ArabicWord.copy(fontSize = 16.sp, color = AyahColors.TextSecondary)) }
        }
    }
}
