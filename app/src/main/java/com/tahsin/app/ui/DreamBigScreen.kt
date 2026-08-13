package com.tahsin.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.tahsin.app.data.dreambig.DreamBigVideo
import com.tahsin.app.data.dreambig.TranscriptParagraph
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonSize
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahLoadingView
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.util.FontStore
import java.util.Locale

/**
 * Game "Dream BIG": peta level (Day 1..10) → kuis kosakata → hasil → materi
 * (transkrip hari itu). Soal dari kosakata kurasi existing.
 */
@Composable
fun DreamBigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: DreamBigViewModel = viewModel(factory = dreamBigViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.dreamBigLoading)
            state.mode == DreamBigMode.LEVELS -> DreamBigLevelMap(
                state = state,
                strings = strings,
                onBack = onBack,
                onPlay = viewModel::startLevel,
                onMateri = viewModel::openTranscript,
            )
            state.mode == DreamBigMode.QUIZ -> DreamBigQuizView(
                state = state,
                strings = strings,
                onBack = viewModel::backToLevels,
                onAnswer = viewModel::answer,
                onNext = viewModel::next,
                arabicFamily = arabicFamily,
            )
            state.mode == DreamBigMode.RESULT -> DreamBigResultView(
                state = state,
                strings = strings,
                onBack = viewModel::backToLevels,
                onRepeat = viewModel::repeatLevel,
                onNextLevel = viewModel::nextLevel,
                onMateri = { viewModel.openTranscript(state.resultDay) },
            )
            else -> DreamBigTranscriptView(
                state = state,
                strings = strings,
                onBack = viewModel::backFromTranscript,
                onOpenVideo = viewModel::openTranscriptVideo,
                context = context,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Peta level
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigLevelMap(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onMateri: (Int) -> Unit,
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

        Spacer(modifier = Modifier.height(16.dp))

        if (state.levels.isEmpty()) {
            AyahText(
                strings.gameEmpty,
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
            )
        } else {
            state.levels.forEach { level ->
                LevelRow(level = level, strings = strings, onPlay = onPlay, onMateri = onMateri)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LevelRow(
    level: DreamBigLevelUi,
    strings: Strings,
    onPlay: (Int) -> Unit,
    onMateri: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AyahCard(
            onClick = { if (!level.locked) onPlay(level.day) },
            modifier = Modifier.weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(
                        level.title,
                        style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (level.locked) {
                        AyahText(
                            strings.gameLocked.format(level.day - 1),
                            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                        )
                    } else {
                        AyahText(
                            stars(level.stars) + "  " + strings.gameBestScore.format(
                                level.bestScore,
                                DreamBigGame.QUESTIONS_PER_LEVEL,
                            ),
                            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                        )
                    }
                }
                if (level.locked) {
                    AyahText("🔒", style = AyahTypography.Body1)
                } else {
                    AyahText(
                        "▶",
                        style = AyahTypography.Body1.copy(
                            color = AyahColors.Primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
        // Materi selalu tersedia walau level terkunci.
        AyahButton(
            text = "📖",
            variant = AyahButtonVariant.Outline,
            size = AyahButtonSize.Small,
            onClick = { onMateri(level.day) },
            modifier = Modifier.align(Alignment.CenterVertically),
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
// Hasil
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigResultView(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    onNextLevel: () -> Unit,
    onMateri: () -> Unit,
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
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            if (state.resultPassed) strings.gamePass else strings.gameFail.format(DreamBigGame.PASS_SCORE),
            style = AyahTypography.Body1.copy(
                color = if (state.resultPassed) AyahColors.Success else AyahColors.Error,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        AyahCard(modifier = Modifier.fillMaxWidth()) {
            ResultLine(strings.gameResultScore.format(state.resultScore, DreamBigGame.QUESTIONS_PER_LEVEL))
            Spacer(modifier = Modifier.height(6.dp))
            ResultLine(strings.gameResultStreak.format(state.resultBestStreak))
            Spacer(modifier = Modifier.height(6.dp))
            ResultLine("Day ${state.resultDay}")
        }
        Spacer(modifier = Modifier.height(24.dp))

        AyahButton(
            text = strings.gameRepeat,
            variant = AyahButtonVariant.Secondary,
            onClick = onRepeat,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (state.resultPassed) {
            AyahButton(
                text = strings.gameNextLevel,
                onClick = onNextLevel,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        AyahButton(
            text = strings.gameMateriToday,
            variant = AyahButtonVariant.Outline,
            onClick = onMateri,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        AyahButton(
            text = strings.gameBackToMap,
            variant = AyahButtonVariant.Ghost,
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
        style = AyahTypography.Body1,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Materi (transkrip)
// ---------------------------------------------------------------------------

@Composable
private fun DreamBigTranscriptView(
    state: DreamBigUiState,
    strings: Strings,
    onBack: () -> Unit,
    onOpenVideo: (DreamBigVideo) -> Unit,
    context: Context,
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
                strings.gameMateri,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // ---- Pilih video hari ini ----
        if (state.transcriptVideos.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.transcriptVideos.forEach { video ->
                    val selected = video.videoId == state.transcriptVideo?.videoId
                    AyahButton(
                        text = if (video.part > 0) "Part ${video.part}" else "Part 1",
                        variant = if (selected) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
                        size = AyahButtonSize.Small,
                        onClick = { onOpenVideo(video) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val video = state.transcriptVideo ?: return
        AyahText(
            video.title,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(12.dp))

        AyahButton(
            text = strings.dreamBigOpenYouTube,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
            },
            size = AyahButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.transcriptParagraphs.isEmpty()) {
            AyahText(
                strings.gameNoVideo,
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
            )
        } else {
            state.transcriptParagraphs.forEach { p ->
                TranscriptRow(p)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Satu paragraf transkrip: timestamp + teks. */
@Composable
private fun TranscriptRow(paragraph: TranscriptParagraph) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AyahText(
            formatTime(paragraph.startMs),
            style = AyahTypography.Body2.copy(
                color = AyahColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.width(52.dp),
        )
        AyahText(
            paragraph.text,
            style = AyahTypography.Body1,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Bantuan
// ---------------------------------------------------------------------------

/** "★★☆" dari jumlah bintang (0..3). */
internal fun stars(count: Int): String = "★".repeat(count.coerceIn(0, 3)) + "☆".repeat((3 - count).coerceIn(0, 3))

/** Format millis → "m:ss" atau "h:mm:ss" (mis. "12:34"). */
internal fun formatTime(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}
