@file:Suppress("MaxLineLength")

package org.opennur.tahsin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.nahwu.NahwuChoiceExercise
import org.opennur.tahsin.data.nahwu.NahwuExercise
import org.opennur.tahsin.data.nahwu.NahwuRearrangeExercise
import org.opennur.tahsin.data.nahwu.NahwuRule
import org.opennur.tahsin.data.nahwu.NahwuWord
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
fun NahwuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: NahwuViewModel = viewModel()
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = androidx.compose.runtime.remember {
        FontStore(context).loadFamily(ArabicFont.UTSMANI)
    }

    BackHandler(enabled = state.mode != NahwuMode.HOME) { viewModel.backToHome() }
    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.nahwuLoading)
            state.mode == NahwuMode.HOME -> NahwuHome(
                state = state,
                strings = strings,
                onBack = onBack,
                onStart = viewModel::startExercises,
                onOpenLesson = viewModel::openLesson,
            )
            state.mode == NahwuMode.LESSON -> NahwuLessonView(
                state = state,
                strings = strings,
                arabicFamily = arabicFamily,
                onBack = viewModel::backToHome,
            )
            else -> NahwuExercises(
                state = state,
                strings = strings,
                arabicFamily = arabicFamily,
                onBack = viewModel::backToHome,
                onAnswer = viewModel::answerChoice,
                onTapWord = viewModel::tapWord,
                onCheckWords = viewModel::checkWords,
                onNext = viewModel::next,
                onRestart = viewModel::restart,
            )
        }
    }
}

private fun localized(language: AppLanguage, id: String, en: String): String =
    if (language == AppLanguage.EN && en.isNotBlank()) en else id

@Composable
private fun NahwuHome(
    state: NahwuUiState,
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
        Header(title = strings.menuNahwu, onBack = onBack)
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(strings.nahwuSubtitle, style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(16.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(strings.nahwuStart, style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    AyahText(
                        strings.nahwuBestScore.format(state.stats.bestScore, 8),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                    AyahText(
                        strings.nahwuSessionsPlayed.format(state.stats.sessionsPlayed),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                AyahButton(text = "▶", variant = AyahButtonVariant.Outline, onClick = onStart)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AyahText(strings.nahwuMaterialTitle, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(6.dp))
        if (state.levels.isEmpty()) {
            AyahText(strings.nahwuEmpty, style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary))
        } else {
            state.levels.forEach { level ->
                AyahCard {
                    AyahText(
                        localized(state.language, level.titleId, level.titleEn),
                        style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                    )
                    AyahText(
                        level.titleAr,
                        style = AyahTypography.ArabicWord.copy(fontSize = 15.sp, color = AyahColors.TextSecondary),
                    )
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
                                AyahText(
                                    localized(state.language, lesson.titleId, lesson.titleEn),
                                    style = AyahTypography.Body2.copy(fontWeight = FontWeight.Medium),
                                )
                                AyahText(
                                    lesson.titleAr,
                                    style = AyahTypography.ArabicWord.copy(fontSize = 13.sp, color = AyahColors.TextSecondary),
                                )
                            }
                            if (lesson.completed) {
                                AyahText("✓", style = AyahTypography.Body1.copy(color = AyahColors.Success))
                            }
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
private fun NahwuLessonView(
    state: NahwuUiState,
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
        Header(title = localized(state.language, lesson.titleId, lesson.titleEn), onBack = onBack, subtitle = lesson.titleAr)
        Spacer(modifier = Modifier.height(16.dp))
        AyahCard {
            AyahText(strings.nahwuLessonIntro, style = AyahTypography.Body2.copy(color = AyahColors.Primary))
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(localized(state.language, lesson.introId, lesson.introEn), style = AyahTypography.Body1)
        }
        Spacer(modifier = Modifier.height(16.dp))
        lesson.rules.forEach { rule ->
            NahwuRuleCard(rule, state.language, arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NahwuRuleCard(rule: NahwuRule, language: AppLanguage, arabicFamily: FontFamily) {
    AyahCard {
        AyahText(localized(language, rule.titleId, rule.titleEn), style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(localized(language, rule.explanationId, rule.explanationEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            rule.exampleAr,
            style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 21.sp, color = AyahColors.Primary),
        )
        AyahText(rule.exampleLatin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
        AyahText(localized(language, rule.exampleId, rule.exampleEn), style = AyahTypography.Body2)
    }
}

@Composable
@Suppress("LongParameterList")
private fun NahwuExercises(
    state: NahwuUiState,
    strings: Strings,
    arabicFamily: FontFamily,
    onBack: () -> Unit,
    onAnswer: (Int) -> Unit,
    onTapWord: (Int) -> Unit,
    onCheckWords: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Header(title = strings.nahwuExercisesTitle, onBack = onBack)
        if (state.exercisesDone) {
            NahwuResult(state, strings, onRestart, onBack)
            return
        }
        val exercise = state.exercise ?: run {
            AyahText(strings.nahwuEmpty, style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary))
            return
        }
        Spacer(modifier = Modifier.height(12.dp))
        AyahText(
            strings.nahwuProgressLabel.format(state.exerciseIndex + 1, state.total),
            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AyahCard {
            when (exercise) {
                is NahwuChoiceExercise -> {
                    AyahText(localized(state.language, exercise.promptId, exercise.promptEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
                    Spacer(modifier = Modifier.height(4.dp))
                    ArabicPrompt(exercise.promptAr, exercise.promptLatin, arabicFamily)
                    Spacer(modifier = Modifier.height(12.dp))
                    val options = if (state.language == AppLanguage.EN) exercise.optionsEn else exercise.optionsId
                    options.forEachIndexed { index, option ->
                        AnswerOption(
                            text = option,
                            answered = state.selected != null,
                            isCorrect = index == exercise.answerIndex,
                            isSelected = index == state.selected,
                            onClick = { onAnswer(index) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                is NahwuRearrangeExercise -> {
                    AyahText(localized(state.language, exercise.promptId, exercise.promptEn), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
                    Spacer(modifier = Modifier.height(12.dp))
                    WordEditor(state, arabicFamily, onTapWord)
                }
            }
            if (state.correct != null) {
                Spacer(modifier = Modifier.height(6.dp))
                AyahText(
                    if (state.correct == true) strings.nahwuCorrect else strings.nahwuWrong,
                    style = AyahTypography.Body1.copy(
                        color = if (state.correct == true) AyahColors.Success else AyahColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (exercise is NahwuRearrangeExercise && state.correct == null) {
                AyahButton(text = strings.nahwuCheck, onClick = onCheckWords, enabled = state.answerReady, modifier = Modifier.fillMaxWidth())
            } else if (state.correct != null) {
                AyahButton(text = strings.nahwuNext, onClick = onNext, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ArabicPrompt(arabic: String, latin: String, family: FontFamily) {
    AyahText(arabic, style = AyahTypography.ArabicWord.copy(fontFamily = family, fontSize = 22.sp, color = AyahColors.TextPrimary))
    AyahText(latin, style = AyahTypography.Body2.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
}

@Composable
private fun AnswerOption(
    text: String,
    answered: Boolean,
    isCorrect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val container = when {
        answered && isCorrect -> AyahColors.Success
        answered && isSelected -> AyahColors.Error
        else -> AyahColors.SurfaceVariant
    }
    val color = if (answered && (isCorrect || isSelected)) Color.White else AyahColors.TextPrimary
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordEditor(state: NahwuUiState, arabicFamily: FontFamily, onTapWord: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AyahColors.SurfaceVariant)
            .padding(10.dp),
    ) {
        if (state.tappedWords.isEmpty()) {
            AyahText("⋯", style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.tappedWords.forEach { index ->
                    WordChip(state.shownWords.getOrNull(index) ?: return@forEach, arabicFamily, false) { onTapWord(index) }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.shownWords.forEachIndexed { index, word ->
            WordChip(word, arabicFamily, index in state.tappedWords) { onTapWord(index) }
        }
    }
}

@Composable
private fun WordChip(word: NahwuWord, arabicFamily: FontFamily, disabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (disabled) AyahColors.Hairline else AyahColors.PrimarySoft)
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AyahText(word.ar, style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 17.sp))
            AyahText(word.latin, style = AyahTypography.Caption.copy(fontStyle = FontStyle.Italic, color = AyahColors.TextSecondary))
        }
    }
}

@Composable
private fun NahwuResult(
    state: NahwuUiState,
    strings: Strings,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))
    AyahCard {
        AyahText(strings.nahwuResultTitle, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(strings.nahwuScore.format(state.score, state.total), style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(strings.nahwuBestScore.format(state.stats.bestScore, state.total), style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
        Spacer(modifier = Modifier.height(14.dp))
        AyahButton(text = strings.nahwuRestart, onClick = onRestart, variant = AyahButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(text = strings.nahwuBackHome, onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AyahText(title, style = AyahTypography.Heading1)
            subtitle?.let { AyahText(it, style = AyahTypography.ArabicWord.copy(fontSize = 16.sp, color = AyahColors.TextSecondary)) }
        }
    }
}
