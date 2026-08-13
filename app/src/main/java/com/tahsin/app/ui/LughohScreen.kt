package com.tahsin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.data.lughoh.DialogueLine
import com.tahsin.app.data.lughoh.Exercise
import com.tahsin.app.data.lughoh.FillBlankExercise
import com.tahsin.app.data.lughoh.GrammarRule
import com.tahsin.app.data.lughoh.LughohEngine
import com.tahsin.app.data.lughoh.LughohLesson
import com.tahsin.app.data.lughoh.RearrangeExercise
import com.tahsin.app.data.lughoh.TranslateArIdExercise
import com.tahsin.app.data.lughoh.TranslateIdArExercise
import com.tahsin.app.data.lughoh.VocabWord
import com.tahsin.app.data.lughoh.WordChip
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
 * Fitur "📚 Belajar Arab": 3 level × 5 pelajaran ala metodologi Durusul
 * Lughoh. Alur: daftar level/pelajaran → detail (Muhadatsah, Mufrodat,
 * Qawa'id, Tadribat) → sesi latihan tap-based → hasil + progres.
 */
@Composable
fun LughohScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: LughohViewModel = viewModel(factory = lughohViewModelFactory(context))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.lughohLoading)
            state.mode == LughohMode.LEVELS -> LughohLevelsView(
                state = state,
                strings = strings,
                onBack = onBack,
                onOpenLesson = viewModel::openLesson,
            )
            state.mode == LughohMode.LESSON -> LughohLessonView(
                state = state,
                strings = strings,
                onBack = viewModel::backToLevels,
                onStartExercises = viewModel::startExercises,
                arabicFamily = arabicFamily,
            )
            else -> LughohExercisesView(
                state = state,
                strings = strings,
                onBack = viewModel::backToLesson,
                onAnswer = viewModel::answerChoice,
                onTapChip = viewModel::tapRearrangeChip,
                onCheckRearrange = viewModel::checkRearrange,
                onNext = viewModel::next,
                onRestart = viewModel::restartExercises,
                onBackToLesson = viewModel::backToLesson,
                arabicFamily = arabicFamily,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Daftar level & pelajaran
// ---------------------------------------------------------------------------

@Composable
private fun LughohLevelsView(
    state: LughohUiState,
    strings: Strings,
    onBack: () -> Unit,
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

        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                strings.menuLughoh,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            strings.lughohSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.levels.isEmpty()) {
            AyahText(
                strings.lughohEmpty,
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
            )
        } else {
            state.levels.forEach { level ->
                LevelSection(
                    titleId = level.titleId,
                    titleAr = level.titleAr,
                    progress = "${level.completedCount}/${level.lessons.size}",
                    lessons = level.lessons,
                    strings = strings,
                    onOpenLesson = onOpenLesson,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LevelSection(
    titleId: String,
    titleAr: String,
    progress: String,
    lessons: List<LughohLessonUi>,
    strings: Strings,
    onOpenLesson: (String) -> Unit,
) {
    AyahCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    titleId,
                    style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                )
                AyahText(
                    titleAr,
                    style = AyahTypography.ArabicWord.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = AyahColors.TextSecondary,
                    ),
                )
            }
            AyahText(
                "$progress ${strings.lughohCompleted}",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        lessons.forEach { lesson ->
            LessonRow(lesson = lesson, strings = strings, onClick = { onOpenLesson(lesson.id) })
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LughohLessonUi,
    strings: Strings,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AyahColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AyahText(
            if (lesson.completed) "✅" else "📖",
            style = AyahTypography.Body1,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            AyahText(
                lesson.titleId,
                style = AyahTypography.Body2.copy(fontWeight = FontWeight.Medium),
            )
            AyahText(
                lesson.titleAr,
                style = AyahTypography.ArabicWord.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = AyahColors.TextSecondary,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Detail pelajaran: Muhadatsah, Mufrodat, Qawa'id, Tadribat
// ---------------------------------------------------------------------------

@Composable
private fun LughohLessonView(
    state: LughohUiState,
    strings: Strings,
    onBack: () -> Unit,
    onStartExercises: () -> Unit,
    arabicFamily: FontFamily,
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    lesson.titleId,
                    style = AyahTypography.Heading1,
                )
                AyahText(
                    lesson.titleAr,
                    style = AyahTypography.ArabicWord.copy(
                        fontFamily = arabicFamily,
                        fontSize = 16.sp,
                        color = AyahColors.TextSecondary,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Muhadatsah
        SectionHeader(strings.lughohSectionMuhadatsah)
        AyahCard {
            lesson.muhadatsah.forEachIndexed { index, line ->
                DialogueRow(line = line, arabicFamily = arabicFamily)
                if (index < lesson.muhadatsah.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Mufrodat
        SectionHeader(strings.lughohSectionMufrodat)
        lesson.mufrodat.forEach { word ->
            VocabCard(word = word, arabicFamily = arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Qawa'id
        SectionHeader(strings.lughohSectionQawaid)
        lesson.qawaid.forEach { rule ->
            GrammarCard(rule = rule, arabicFamily = arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Tadribat
        SectionHeader(strings.lughohSectionTadribat)
        AyahCard {
            AyahText(
                strings.lughohTadribatHint,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            AyahButton(
                text = strings.lughohStartExercises,
                onClick = onStartExercises,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    AyahText(
        text,
        style = AyahTypography.Heading2,
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun DialogueRow(line: DialogueLine, arabicFamily: FontFamily) {
    Column {
        AyahText(
            line.speaker,
            style = AyahTypography.Caption.copy(
                color = AyahColors.Primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        AyahText(
            line.ar,
            style = AyahTypography.ArabicWord.copy(
                fontFamily = arabicFamily,
                fontSize = 19.sp,
                color = AyahColors.TextPrimary,
            ),
        )
        AyahText(
            line.latin,
            style = AyahTypography.Body2.copy(
                fontStyle = FontStyle.Italic,
                color = AyahColors.TextSecondary,
            ),
        )
        AyahText(
            line.id,
            style = AyahTypography.Body2,
        )
    }
}

@Composable
private fun VocabCard(word: VocabWord, arabicFamily: FontFamily) {
    AyahCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    word.id,
                    style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                )
                AyahText(
                    word.latin,
                    style = AyahTypography.Body2.copy(
                        fontStyle = FontStyle.Italic,
                        color = AyahColors.TextSecondary,
                    ),
                )
            }
            AyahText(
                word.ar,
                style = AyahTypography.ArabicWord.copy(
                    fontFamily = arabicFamily,
                    fontSize = 20.sp,
                    color = AyahColors.TextPrimary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            "— ${word.exampleId}",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        AyahText(
            word.exampleAr,
            style = AyahTypography.ArabicWord.copy(
                fontFamily = arabicFamily,
                fontSize = 16.sp,
                color = AyahColors.TextPrimary,
            ),
        )
        AyahText(
            word.exampleLatin,
            style = AyahTypography.Caption.copy(
                fontStyle = FontStyle.Italic,
                color = AyahColors.TextSecondary,
            ),
        )
    }
}

@Composable
private fun GrammarCard(rule: GrammarRule, arabicFamily: FontFamily) {
    AyahCard {
        AyahText(
            rule.titleId,
            style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            rule.id,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AyahText(
            rule.exampleAr,
            style = AyahTypography.ArabicWord.copy(
                fontFamily = arabicFamily,
                fontSize = 18.sp,
                color = AyahColors.Primary,
            ),
        )
        AyahText(
            rule.exampleLatin,
            style = AyahTypography.Caption.copy(
                fontStyle = FontStyle.Italic,
                color = AyahColors.TextSecondary,
            ),
        )
        AyahText(
            rule.exampleId,
            style = AyahTypography.Caption,
        )
    }
}

// ---------------------------------------------------------------------------
// Sesi latihan (tadribat)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LughohExercisesView(
    state: LughohUiState,
    strings: Strings,
    onBack: () -> Unit,
    onAnswer: (String) -> Unit,
    onTapChip: (Int) -> Unit,
    onCheckRearrange: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onBackToLesson: () -> Unit,
    arabicFamily: FontFamily,
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
                strings.lughohExercisesTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.exercisesDone) {
            ExerciseResult(state = state, strings = strings, onRestart = onRestart, onBackToLesson = onBackToLesson)
            return
        }

        val ex = state.exercise
        if (ex == null) {
            AyahText(
                strings.lughohEmpty,
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
            )
            return
        }

        Spacer(modifier = Modifier.height(12.dp))
        AyahText(
            strings.lughohProgressLabel.format(state.exerciseIndex + 1, state.total),
            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))

        AyahCard {
            // ---- Soal ----
            when (ex) {
                is FillBlankExercise -> FillBlankPrompt(ex, arabicFamily)
                is TranslateArIdExercise -> TranslateArIdPrompt(ex, arabicFamily)
                is TranslateIdArExercise -> TranslateIdArPrompt(ex)
                is RearrangeExercise -> Unit
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Pilihan / susunan kata ----
            when (ex) {
                is RearrangeExercise -> RearrangeEditor(
                    ex = ex,
                    tapped = state.rearrangeTapped,
                    bank = state.rearrangeShown,
                    revealed = state.correct != null,
                    onTapChip = onTapChip,
                    arabicFamily = arabicFamily,
                )
                else -> {
                    val options = when (ex) {
                        is FillBlankExercise -> ex.options
                        is TranslateArIdExercise -> ex.options
                        is TranslateIdArExercise -> ex.options
                        is RearrangeExercise -> emptyList()
                    }
                    val answer = when (ex) {
                        is FillBlankExercise -> ex.answer
                        is TranslateArIdExercise -> ex.answer
                        is TranslateIdArExercise -> ex.answer
                        is RearrangeExercise -> ""
                    }
                    val isArabic = ex is FillBlankExercise || ex is TranslateIdArExercise
                    options.forEach { option ->
                        ExerciseOption(
                            text = option,
                            isArabic = isArabic,
                            answered = state.selected != null,
                            isCorrectOption = option == answer,
                            isSelected = option == state.selected,
                            enabled = state.selected == null,
                            onClick = { onAnswer(option) },
                            arabicFamily = arabicFamily,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // ---- Umpan balik + aksi ----
            val answered = state.correct != null
            if (answered) {
                Spacer(modifier = Modifier.height(6.dp))
                AyahText(
                    if (state.correct == true) strings.lughohCorrect else strings.lughohWrong,
                    style = AyahTypography.Body1.copy(
                        color = if (state.correct == true) AyahColors.Success else AyahColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (ex is RearrangeExercise && !answered) {
                AyahButton(
                    text = strings.lughohCheck,
                    onClick = onCheckRearrange,
                    enabled = state.answerReady,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (answered) {
                AyahButton(
                    text = strings.lughohNext,
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FillBlankPrompt(ex: FillBlankExercise, arabicFamily: FontFamily) {
    AyahText(
        ex.promptId,
        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
    )
    Spacer(modifier = Modifier.height(4.dp))
    AyahText(
        ex.displayPromptAr,
        style = AyahTypography.ArabicWord.copy(
            fontFamily = arabicFamily,
            fontSize = 22.sp,
            color = AyahColors.TextPrimary,
        ),
    )
    AyahText(
        ex.displayPromptLatin,
        style = AyahTypography.Body2.copy(
            fontStyle = FontStyle.Italic,
            color = AyahColors.TextSecondary,
        ),
    )
}

@Composable
private fun TranslateArIdPrompt(ex: TranslateArIdExercise, arabicFamily: FontFamily) {
    AyahText(
        ex.promptAr,
        style = AyahTypography.ArabicWord.copy(
            fontFamily = arabicFamily,
            fontSize = 22.sp,
            color = AyahColors.TextPrimary,
        ),
    )
    AyahText(
        ex.promptLatin,
        style = AyahTypography.Body2.copy(
            fontStyle = FontStyle.Italic,
            color = AyahColors.TextSecondary,
        ),
    )
}

@Composable
private fun TranslateIdArPrompt(ex: TranslateIdArExercise) {
    AyahText(
        ex.promptId,
        style = AyahTypography.Body1.copy(
            fontWeight = FontWeight.SemiBold,
            color = AyahColors.TextPrimary,
        ),
    )
}

/** Editor susun kata: area jawaban di atas, bank kata di bawah.
 *  [tapped] = indeks (di [bank]) urutan kata yang sudah dipilih; pakai indeks
 *  supaya aman untuk kata yang sama muncul dua kali. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RearrangeEditor(
    ex: RearrangeExercise,
    tapped: List<Int>,
    bank: List<WordChip>,
    revealed: Boolean,
    onTapChip: (Int) -> Unit,
    arabicFamily: FontFamily,
) {
    // Area jawaban
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AyahColors.SurfaceVariant)
            .padding(10.dp),
    ) {
        if (tapped.isEmpty()) {
            AyahText(
                "⋯",
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tapped.forEachIndexed { position, index ->
                    val chip = bank.getOrNull(index) ?: return@forEachIndexed
                    WordChipView(
                        chip = chip,
                        arabicFamily = arabicFamily,
                        selected = true,
                        correct = if (revealed) {
                            LughohEngine.isChipAtPositionCorrect(ex, chip, position)
                        } else null,
                        onClick = { onTapChip(index) },
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Bank kata
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        bank.forEachIndexed { index, chip ->
            WordChipView(
                chip = chip,
                arabicFamily = arabicFamily,
                selected = false,
                correct = null,
                onClick = { onTapChip(index) },
                disabled = index in tapped,
            )
        }
    }
}

@Composable
private fun WordChipView(
    chip: WordChip,
    arabicFamily: FontFamily,
    selected: Boolean,
    correct: Boolean?,
    onClick: () -> Unit,
    disabled: Boolean = false,
) {
    val container = when {
        !selected && disabled -> AyahColors.Hairline
        correct == true -> AyahColors.Success
        correct == false -> AyahColors.Error
        selected -> AyahColors.PrimarySoft
        else -> AyahColors.SurfaceVariant
    }
    val content = when {
        correct != null -> Color.White
        else -> AyahColors.TextPrimary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(enabled = !disabled && !selected, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AyahText(
                chip.ar,
                style = AyahTypography.ArabicWord.copy(
                    fontFamily = arabicFamily,
                    fontSize = 17.sp,
                    color = content,
                ),
            )
            AyahText(
                chip.latin,
                style = AyahTypography.Caption.copy(
                    fontStyle = FontStyle.Italic,
                    color = if (correct != null) Color.White else AyahColors.TextSecondary,
                ),
            )
        }
    }
}

/** Opsi pilihan ganda: identik pola QuizOption di Dream Big. */
private enum class LughohOptionState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun ExerciseOption(
    text: String,
    isArabic: Boolean,
    answered: Boolean,
    isCorrectOption: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    arabicFamily: FontFamily,
) {
    val state = when {
        !answered -> LughohOptionState.Idle
        isCorrectOption -> LughohOptionState.Correct
        isSelected -> LughohOptionState.Wrong
        else -> LughohOptionState.Dimmed
    }
    val container = when (state) {
        LughohOptionState.Idle -> AyahColors.SurfaceVariant
        LughohOptionState.Correct -> AyahColors.Success
        LughohOptionState.Wrong -> AyahColors.Error
        LughohOptionState.Dimmed -> AyahColors.SurfaceVariant
    }
    val content = when (state) {
        LughohOptionState.Correct, LughohOptionState.Wrong -> Color.White
        LughohOptionState.Dimmed -> AyahColors.TextSecondary
        LughohOptionState.Idle -> AyahColors.TextPrimary
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

/** Hasil sesi latihan: skor + aksi ulangi / kembali ke materi. */
@Composable
private fun ExerciseResult(
    state: LughohUiState,
    strings: Strings,
    onRestart: () -> Unit,
    onBackToLesson: () -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))
    AyahCard {
        AyahText(
            strings.lughohResultTitle,
            style = AyahTypography.Heading2,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(
            strings.lughohScore.format(state.score, state.total),
            style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(
            strings.lughohLessonCompleted,
            style = AyahTypography.Body2.copy(color = AyahColors.Success),
        )
        Spacer(modifier = Modifier.height(14.dp))
        AyahButton(
            text = strings.lughohRestart,
            onClick = onRestart,
            variant = AyahButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = strings.lughohBackToLesson,
            onClick = onBackToLesson,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}
