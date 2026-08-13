package com.tahsin.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahLoadingView
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.util.FontStore

/**
 * Game "Dream BIG" (arcade): ronde kuis kosakata acak yang bisa dimainkan
 * terus. Alur: HOME (rekor + tombol main) → QUIZ → RESULT (skor + streak).
 */
@Composable
fun DreamBigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: DreamBigViewModel = viewModel(factory = dreamBigViewModelFactory(context))
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.dreamBigLoading)
            state.mode == DreamBigMode.HOME -> DreamBigHomeView(
                state = state,
                strings = strings,
                onBack = onBack,
                onStart = viewModel::startRound,
            )
            state.mode == DreamBigMode.QUIZ -> DreamBigQuizView(
                state = state,
                strings = strings,
                onBack = viewModel::backToHome,
                onAnswer = viewModel::answer,
                onNext = viewModel::next,
                arabicFamily = arabicFamily,
            )
            else -> DreamBigResultView(
                state = state,
                strings = strings,
                onBack = viewModel::backToHome,
                onRepeat = viewModel::playAgain,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Home (arcade): rekor + tombol main
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigHomeView(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
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
                strings.dreamBigTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            strings.dreamBigSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---- Rekor ----
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell(
                    label = strings.gameBestScore.format(
                        state.stats.bestScore,
                        DreamBigGame.QUESTIONS_PER_ROUND,
                    ),
                    value = "${state.stats.bestScore}",
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = strings.gameResultStreak.format(state.stats.bestStreak),
                    value = "${state.stats.bestStreak}",
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = strings.gameRoundsPlayed.format(state.stats.roundsPlayed),
                    value = "${state.stats.roundsPlayed}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AyahButton(
            text = strings.gameStart,
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AyahText(
            value,
            style = AyahTypography.Heading2.copy(color = AyahColors.Primary),
        )
        AyahText(
            label,
            style = AyahTypography.Caption.copy(
                color = AyahColors.TextSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// Kuis
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigQuizView(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    arabicFamily: FontFamily,
) {
    val quiz = state.quiz ?: return
    val q = quiz.question

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Header: back + progres ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                strings.gameQuestionProgress.format(quiz.index + 1, quiz.total),
                style = AyahTypography.Heading2,
                modifier = Modifier.weight(1f),
            )
            AyahText(
                strings.gameScoreLabel.format(quiz.score),
                style = AyahTypography.Body1.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(
                strings.gameStreakLabel.format(quiz.streak),
                style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // ---- Soal ----
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            if (q.promptTranslit.isNotEmpty()) {
                // Arabic → meaning: tampilkan kata Arab besar + transliterasi.
                AyahText(
                    q.prompt,
                    style = AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 30.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    q.promptTranslit,
                    style = AyahTypography.Transliteration.copy(color = AyahColors.TextSecondary),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // Reverse: tampilkan artinya.
                AyahText(
                    q.prompt,
                    style = AyahTypography.Body1.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Opsi ----
        val answered = quiz.selected != null
        q.options.forEach { option ->
            QuizOption(
                text = option,
                isArabic = q.promptTranslit.isEmpty(),
                state = when {
                    !answered -> OptionState.Idle
                    option == q.options[q.correctIndex] -> OptionState.Correct
                    option == quiz.selected -> OptionState.Wrong
                    else -> OptionState.Dimmed
                },
                enabled = !answered,
                onClick = { onAnswer(option) },
                arabicFamily = arabicFamily,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ---- Umpan balik + lanjut ----
        if (answered) {
            Spacer(modifier = Modifier.height(6.dp))
            AyahText(
                if (quiz.correct == true) strings.gameCorrect else strings.gameWrong,
                style = AyahTypography.Body1.copy(
                    color = if (quiz.correct == true) AyahColors.Success else AyahColors.Error,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            AyahButton(
                text = strings.gameNext,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private enum class OptionState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun QuizOption(
    text: String,
    isArabic: Boolean,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    arabicFamily: FontFamily,
) {
    val container = when (state) {
        OptionState.Idle -> AyahColors.SurfaceVariant
        OptionState.Correct -> AyahColors.Success
        OptionState.Wrong -> AyahColors.Error
        OptionState.Dimmed -> AyahColors.SurfaceVariant
    }
    val content = when (state) {
        OptionState.Correct, OptionState.Wrong -> Color.White
        OptionState.Dimmed -> AyahColors.TextSecondary
        OptionState.Idle -> AyahColors.TextPrimary
    }
    val style = if (isArabic) {
        AyahTypography.ArabicWord.copy(fontFamily = arabicFamily, fontSize = 18.sp, color = content)
    } else {
        AyahTypography.Body1.copy(fontWeight = FontWeight.Medium, color = content)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        AyahText(text, style = style, modifier = Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------------------
// Hasil ronde
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigResultView(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        AyahText(
            strings.gameResultTitle,
            style = AyahTypography.Heading1,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AyahText(
            stars(state.resultStars),
            style = AyahTypography.Heading1.copy(
                color = AyahColors.Secondary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))

        AyahCard(modifier = Modifier.fillMaxWidth()) {
            ResultLine(strings.gameResultScore.format(state.resultScore, DreamBigGame.QUESTIONS_PER_ROUND))
            Spacer(modifier = Modifier.height(6.dp))
            ResultLine(strings.gameResultStreak.format(state.resultBestStreak))
        }
        Spacer(modifier = Modifier.height(24.dp))

        AyahButton(
            text = strings.gameRepeat,
            onClick = onRepeat,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = strings.gameBackHome,
            variant = AyahButtonVariant.Outline,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultLine(text: String) {
    AyahText(
        text,
        style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Label bintang: 1★ per 40%, 2★ per 60%, 3★ per 80%. */
private fun stars(count: Int): String = "★".repeat(count).ifEmpty { "☆" }
