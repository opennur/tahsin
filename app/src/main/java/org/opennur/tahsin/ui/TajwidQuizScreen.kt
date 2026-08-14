package org.opennur.tahsin.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.tajwid.QuizQuestion
import org.opennur.tahsin.data.tajwid.TajwidQuiz
import org.opennur.tahsin.theme.ArabicFont
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.FontStore

/**
 * Layar kuis tajwid: "hukum apa pada kata ini?" — kata acak dari seluruh
 * mushaf, 4 pilihan ganda, umpan balik + penjelasan hukum, dan skor.
 */
@Composable
fun TajwidQuizScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: TajwidQuizViewModel = viewModel()
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

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
                    strings.quizTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Skor ----
            AyahText(
                strings.quizScore.format(state.correctCount, state.totalCount),
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.loading -> StatusHint(strings.quizLoading)
                state.question == null -> StatusHint(strings.quizNoData)
                else -> QuestionCard(
                    question = state.question!!,
                    selected = state.selected,
                    ayahLabel = state.ayahLabel,
                    translation = state.translation,
                    language = state.language,
                    strings = strings,
                    fontFamily = arabicFamily,
                    onAnswer = viewModel::answer,
                    onNext = viewModel::next,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Kartu satu soal + opsi + umpan balik. */
@Composable
private fun QuestionCard(
    question: QuizQuestion,
    selected: String?,
    ayahLabel: String,
    translation: String,
    language: AppLanguage,
    strings: Strings,
    fontFamily: FontFamily,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
) {
    val answered = selected != null
    val isCorrectPick = answered && TajwidQuiz.isCorrect(selected!!, question)

    AyahCard(modifier = Modifier.fillMaxWidth()) {
        if (ayahLabel.isNotBlank()) {
            AyahText(
                ayahLabel,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Kata sasaran + konteks kata sekitar
        Row(verticalAlignment = Alignment.CenterVertically) {
            question.prevWord?.let { prev ->
                AyahText(
                    prev,
                    style = AyahTypography.Arabic.copy(
                        fontSize = 14.sp,
                        color = AyahColors.TextSecondary,
                        fontFamily = fontFamily,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            AyahText(
                question.word,
                style = AyahTypography.Arabic.copy(
                    fontSize = 26.sp,
                    color = AyahColors.TextPrimary,
                    fontFamily = fontFamily,
                ),
            )
            question.nextWord?.let { next ->
                Spacer(modifier = Modifier.width(8.dp))
                AyahText(
                    next,
                    style = AyahTypography.Arabic.copy(
                        fontSize = 14.sp,
                        color = AyahColors.TextSecondary,
                        fontFamily = fontFamily,
                    ),
                )
            }
        }

        if (translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                translation,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                maxLines = 2,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        AyahText(strings.quizQuestion, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(10.dp))

        // Opsi pilihan ganda
        question.options.forEach { option ->
            val variant = when {
                !answered -> AyahButtonVariant.Outline
                option == question.targetRule.name -> AyahButtonVariant.Primary
                option == selected -> AyahButtonVariant.Danger
                else -> AyahButtonVariant.Outline
            }
            AyahButton(
                text = option,
                variant = variant,
                onClick = { onAnswer(option) },
                enabled = !answered,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    // ---- Umpan balik setelah menjawab ----
    if (answered) {
        Spacer(modifier = Modifier.height(12.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            val target = question.targetRule
            AyahText(
                if (isCorrectPick) strings.quizCorrect else strings.quizWrong,
                style = AyahTypography.Body2.copy(
                    color = if (isCorrectPick) AyahColors.Success else AyahColors.Error,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                "${strings.quizRuleLabel}: ${target.name}",
                style = AyahTypography.Body2.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            val explanation = if (language == AppLanguage.EN) target.explanationEn else target.explanation
            AyahText(
                explanation,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        AyahButton(
            text = strings.quizNext,
            variant = AyahButtonVariant.Primary,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Kartu status (memuat / gagal menyiapkan soal). */
@Composable
private fun StatusHint(message: String) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        AyahText(
            message,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
