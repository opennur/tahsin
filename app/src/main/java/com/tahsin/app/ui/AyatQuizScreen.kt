package com.tahsin.app.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.data.ayatquiz.AyatQuiz
import com.tahsin.app.data.ayatquiz.SurahQuiz
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.FontStore

/**
 * Layar "Kuis Ayat": dua mode pilihan ganda dari seluruh mushaf —
 * Lengkapi Ayat (kata mana yang melengkapi?) & Tebak Surah (dari surah apa?).
 */
@Composable
fun AyatQuizScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: AyatQuizViewModel = viewModel(factory = ayatQuizViewModelFactory(context))
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
                    strings.ayatQuizTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ---- Pilihan mode ----
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                AyahButton(
                    text = strings.ayatQuizModeComplete,
                    variant = if (state.mode == AyatQuizMode.COMPLETE) AyahButtonVariant.Primary
                    else AyahButtonVariant.Outline,
                    onClick = { viewModel.setMode(AyatQuizMode.COMPLETE) },
                    modifier = Modifier.weight(1f),
                )
                AyahButton(
                    text = strings.ayatQuizModeSurah,
                    variant = if (state.mode == AyatQuizMode.SURAH) AyahButtonVariant.Primary
                    else AyahButtonVariant.Outline,
                    onClick = { viewModel.setMode(AyatQuizMode.SURAH) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                state.mode == AyatQuizMode.COMPLETE && state.completeQuestion == null -> StatusHint(strings.quizNoData)
                state.mode == AyatQuizMode.SURAH && state.surahQuestion == null -> StatusHint(strings.quizNoData)
                else -> QuestionCard(
                    state = state,
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

/** Kartu satu soal + opsi + umpan balik (mengikuti pola Kuis Tajwid). */
@Composable
private fun QuestionCard(
    state: AyatQuizUiState,
    strings: Strings,
    fontFamily: FontFamily,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
) {
    val answered = state.selected != null
    val isCorrectPick = answered && when (state.mode) {
        AyatQuizMode.COMPLETE -> AyatQuiz.isCorrect(state.selected!!, state.completeQuestion!!)
        AyatQuizMode.SURAH -> SurahQuiz.isCorrect(state.selected!!, state.surahQuestion!!)
    }
    val options: List<String> = when (state.mode) {
        AyatQuizMode.COMPLETE -> state.completeQuestion!!.options
        AyatQuizMode.SURAH -> state.surahQuestion!!.options
    }
    val questionText = when (state.mode) {
        AyatQuizMode.COMPLETE -> strings.ayatQuizCompleteQuestion
        AyatQuizMode.SURAH -> strings.ayatQuizSurahQuestion
    }
    val prompt = when (state.mode) {
        AyatQuizMode.COMPLETE -> state.completeQuestion!!.blankedText
        AyatQuizMode.SURAH -> state.surahQuestion!!.fragment
    }

    AyahCard(modifier = Modifier.fillMaxWidth()) {
        if (state.ayahLabel.isNotBlank()) {
            AyahText(
                state.ayahLabel,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        AyahText(
            prompt,
            style = AyahTypography.Arabic.copy(
                fontSize = 24.sp,
                color = AyahColors.TextPrimary,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                state.translation,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                maxLines = 2,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        AyahText(questionText, style = AyahTypography.Heading2)
        Spacer(modifier = Modifier.height(10.dp))

        options.forEach { option ->
            val variant = when {
                !answered -> AyahButtonVariant.Outline
                option == state.selected && isCorrectPick -> AyahButtonVariant.Primary
                option == state.selected -> AyahButtonVariant.Danger
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

    if (answered) {
        Spacer(modifier = Modifier.height(12.dp))
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            AyahText(
                if (isCorrectPick) strings.quizCorrect else strings.quizWrong,
                style = AyahTypography.Body2.copy(
                    color = if (isCorrectPick) AyahColors.Success else AyahColors.Error,
                    fontWeight = FontWeight.SemiBold,
                ),
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
