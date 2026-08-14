package org.opennur.tahsin.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.theme.ArabicFont
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahShapes
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.FontStore

/**
 * Layar belajar kosa kata Al-Qur'an — dua mode:
 * - Kartu: kata Arab + transliterasi, ketuk untuk melihat arti, contoh ayat
 *   (bisa dibuka di mushaf), audio kata, tombol Ingat/Lupa (SRS).
 * - Kuis: pilihan ganda "arti kata ini?" / "kata mana yang artinya X".
 */
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: VocabularyViewModel = viewModel()
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    // Hentikan audio kata begitu layar kosa kata ditutup (audio milik instance
    // pemutar sendiri, bukan pemutar layar utama).
    DisposableEffect(viewModel) {
        onDispose { viewModel.stopAudio() }
    }

    // Back sistem: kalau sedang di Kuis, kembali ke mode Kartu dulu — baru
    // keluar ke menu utama (BackHandler global di MainActivity).
    BackHandler(enabled = state.mode != VocabMode.CARDS) {
        viewModel.switchMode(VocabMode.CARDS)
    }

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
                AyahButton(
                    text = "←",
                    variant = AyahButtonVariant.Outline,
                    // Konsisten dengan back sistem: di Kuis balik ke Kartu dulu.
                    onClick = if (state.mode == VocabMode.CARDS) {
                        onBack
                    } else {
                        { viewModel.switchMode(VocabMode.CARDS) }
                    },
                )
                Spacer(modifier = Modifier.width(12.dp))
                AyahText(
                    strings.vocabTitle,
                    style = AyahTypography.Heading1,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                strings.vocabSubtitle,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Pilih mode: Kartu / Kuis ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AyahButton(
                    text = strings.vocabModeCards,
                    variant = if (state.mode == VocabMode.CARDS) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.switchMode(VocabMode.CARDS) },
                )
                AyahButton(
                    text = strings.vocabModeQuiz,
                    variant = if (state.mode == VocabMode.QUIZ) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.switchMode(VocabMode.QUIZ) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Progres global ----
            AyahText(
                "${strings.vocabLearned.format(state.learnedCount)} · ${strings.vocabDue.format(state.dueCount)}",
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.loading -> StatusHint(strings.vocabLoading)
                state.mode == VocabMode.CARDS -> CardsSection(
                    state = state,
                    strings = strings,
                    arabicFamily = arabicFamily,
                    onFlip = viewModel::flip,
                    onAnswer = viewModel::answerCard,
                    onPlay = viewModel::playCurrentWord,
                    onStartQuiz = { viewModel.switchMode(VocabMode.QUIZ) },
                    onOpenAyah = onOpenAyah,
                )
                else -> QuizSection(
                    state = state,
                    strings = strings,
                    onAnswer = viewModel::answerQuiz,
                    onNext = viewModel::nextQuiz,
                    onRestart = viewModel::restartQuiz,
                    onBackToCards = { viewModel.switchMode(VocabMode.CARDS) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ---- Banner pesan (mis. audio tidak tersedia) ----
        state.message?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(AyahColors.Surface, AyahShapes.Button)
                    .clickable(onClick = viewModel::dismissMessage)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AyahText(
                    "$message  ✕",
                    style = AyahTypography.Body2.copy(
                        color = AyahColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/** Bagian mode kartu: kartu kata + contoh ayat + tombol Ingat/Lupa. */
@Composable
private fun CardsSection(
    state: VocabUiState,
    strings: Strings,
    arabicFamily: FontFamily,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onStartQuiz: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
) {
    if (state.session.isEmpty()) {
        StatusHint(strings.vocabEmpty)
        return
    }
    val entry = state.current
    if (entry == null) {
        // Sesi kartu selesai.
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            AyahText(
                strings.vocabSessionDone,
                style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "${strings.vocabLearned.format(state.learnedCount)} · ${strings.vocabDue.format(state.dueCount)}",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            AyahButton(text = strings.vocabStartQuiz, onClick = onStartQuiz)
        }
        return
    }

    AyahText(
        "${state.answeredCount + 1}/${state.session.size}",
        style = AyahTypography.Caption.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(modifier = Modifier.height(8.dp))

    // ---- Kartu kata (ketuk untuk membalik) ----
    AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onFlip) {
        AyahText(
            entry.word,
            style = TextStyle(
                fontSize = 36.sp,
                fontFamily = arabicFamily,
                color = AyahColors.TextPrimary,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(
            entry.translit,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (state.flipped) {
            // ---- Sisi belakang: arti + contoh ayat ----
            val ex = entry.example
            AyahText(
                strings.vocabMeaning,
                style = AyahTypography.Overline.copy(color = AyahColors.Primary, fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                if (state.language == AppLanguage.EN) entry.meaningEn else entry.meaningId,
                style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
            )
            if (entry.root.isNotBlank() && entry.root != entry.key) {
                Spacer(modifier = Modifier.height(8.dp))
                AyahText(
                    "${strings.vocabRoot}: ${entry.root}" +
                        if (state.language == AppLanguage.EN) " — ${entry.rootMeaningEn}"
                        else " — ${entry.rootMeaningId}",
                    style = AyahTypography.Body2.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            if (ex.surah > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                AyahText(
                    strings.vocabExample,
                    style = AyahTypography.Overline.copy(color = AyahColors.Primary, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    ex.ayahArab,
                    style = TextStyle(fontSize = 20.sp, fontFamily = arabicFamily, color = AyahColors.TextPrimary),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    ex.ayahLatin,
                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    if (state.language == AppLanguage.EN) ex.ayahEn else ex.ayahId,
                    style = AyahTypography.Body2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AyahText(
                    "${strings.vocabOpenExample}  ${ex.surah}:${ex.ayah}",
                    style = AyahTypography.Body2.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAyah(ex.surah, ex.ayah) },
                )
            }
        } else {
            AyahText(
                strings.vocabFlipHint,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    AyahButton(
        text = strings.vocabPlayAudio,
        variant = AyahButtonVariant.Outline,
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay,
    )

    if (state.flipped) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AyahButton(
                text = strings.vocabForgot,
                variant = AyahButtonVariant.Danger,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(false) },
            )
            AyahButton(
                text = strings.vocabRemember,
                variant = AyahButtonVariant.Primary,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(true) },
            )
        }
    }
}

/** Bagian mode kuis: pilihan ganda + umpan balik + skor. */
@Composable
private fun QuizSection(
    state: VocabUiState,
    strings: Strings,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onBackToCards: () -> Unit,
) {
    if (state.quizDone) {
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            AyahText(
                strings.vocabQuizDone.format(state.quizCorrect, state.quizTotal),
                style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AyahButton(
                    text = strings.vocabModeCards,
                    variant = AyahButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                    onClick = onBackToCards,
                )
                AyahButton(
                    text = strings.vocabQuizAgain,
                    modifier = Modifier.weight(1f),
                    onClick = onRestart,
                )
            }
        }
        return
    }

    val question = state.question
    if (question == null) {
        StatusHint(strings.vocabLoading)
        return
    }

    AyahText(
        strings.vocabQuizScore.format(state.quizCorrect, state.quizTotal),
        style = AyahTypography.Caption.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(modifier = Modifier.height(12.dp))

    val answered = state.selected != null
    val isForward = question.promptTranslit.isNotBlank()
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        AyahText(
            if (isForward) strings.vocabQuizQuestionWord else strings.vocabQuizQuestionMeaning,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isForward) {
            AyahText(
                question.prompt,
                style = TextStyle(
                    fontSize = 30.sp,
                    color = AyahColors.TextPrimary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                question.promptTranslit,
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AyahText(
                question.prompt,
                style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        question.options.forEach { option ->
            val isCorrectOption = option == question.options[question.correctIndex]
            val isPicked = option == state.selected
            val variant = when {
                !answered -> AyahButtonVariant.Outline
                isCorrectOption -> AyahButtonVariant.Primary
                isPicked -> AyahButtonVariant.Danger
                else -> AyahButtonVariant.Outline
            }
            AyahButton(
                text = option,
                variant = variant,
                size = AyahButtonSize.Default,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAnswer(option) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (answered) {
            val pickedCorrect = state.selected == question.options[question.correctIndex]
            AyahText(
                if (pickedCorrect) strings.vocabQuizCorrect
                else strings.vocabQuizWrong.format(question.options[question.correctIndex]),
                style = AyahTypography.Body2.copy(
                    color = if (pickedCorrect) AyahColors.Success else AyahColors.Error,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AyahButton(
                text = strings.vocabQuizNext,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNext,
            )
        }
    }
}

/** Kartu status sederhana (memuat / kosong). */
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
