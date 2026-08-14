package org.opennur.tahsin.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
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
import org.opennur.tahsin.data.lughoh.DialogueLine
import org.opennur.tahsin.data.lughoh.Exercise
import org.opennur.tahsin.data.lughoh.FillBlankExercise
import org.opennur.tahsin.data.lughoh.GrammarRule
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.data.lughoh.LughohLesson
import org.opennur.tahsin.data.lughoh.RearrangeExercise
import org.opennur.tahsin.data.lughoh.TranslateArIdExercise
import org.opennur.tahsin.data.lughoh.TranslateIdArExercise
import org.opennur.tahsin.data.lughoh.VocabWord
import org.opennur.tahsin.data.lughoh.WordChip
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

/**
 * Fitur "📚 Belajar Arab": latihan acak tak terbatas (arcade) dari seluruh
 * pelajaran + browser materi (Muhadatsah/Mufrodat/Qawa'id) yang tetap bisa
 * dibaca. Alur: HOME (arcade + materi) → LESSON (baca materi) / EXERCISES.
 */
@Composable
fun LughohScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: LughohViewModel = viewModel()
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    val arabicFamily = remember { FontStore(context).loadFamily(ArabicFont.UTSMANI) }

    // Back sistem: kalau sedang di sub-tampilan Lughoh (Materi/Latihan), kembali
    // ke halaman awal Lughoh dulu — baru keluar ke menu utama (ditangani
    // MainActivity). BackHandler terdalam menang sebelum BackHandler global.
    BackHandler(enabled = state.mode != LughohMode.HOME) { viewModel.backToHome() }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        when {
            state.loading -> AyahLoadingView(message = strings.lughohLoading)
            state.mode == LughohMode.HOME -> LughohHomeView(
                state = state,
                strings = strings,
                language = state.language,
                onBack = onBack,
                onStart = viewModel::startRandomExercises,
                onOpenLesson = viewModel::openLesson,
            )
            state.mode == LughohMode.LESSON -> LughohLessonView(
                state = state,
                strings = strings,
                onBack = viewModel::backToHome,
                arabicFamily = arabicFamily,
            )
            else -> LughohExercisesView(
                state = state,
                strings = strings,
                onBack = viewModel::backToHome,
                onAnswer = viewModel::answerChoice,
                onTapChip = viewModel::tapRearrangeChip,
                onCheckRearrange = viewModel::checkRearrange,
                onNext = viewModel::next,
                onRestart = viewModel::restartExercises,
                onBackToHome = viewModel::backToHome,
                arabicFamily = arabicFamily,
            )
        }
    }
}

/** Pilih teks sesuai bahasa: EN jika tersedia, selain itu fallback ID. */
private fun textOf(language: AppLanguage, id: String, en: String): String =
    if (language == AppLanguage.EN && en.isNotBlank()) en else id

// ---------------------------------------------------------------------------
// Halaman awal: kartu latihan acak + browser materi
// ---------------------------------------------------------------------------

@Composable
private fun LughohHomeView(
    state: LughohUiState,
    strings: Strings,
    language: AppLanguage,
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

        // ---- Kartu arcade: latihan acak ----
        AyahCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AyahText(
                        strings.lughohStartRandom,
                        style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AyahText(
                        strings.lughohBestScore.format(
                            state.stats.bestScore,
                            LughohEngine.SESSION_SIZE,
                        ),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                    AyahText(
                        strings.lughohSessionsPlayed.format(state.stats.roundsPlayed),
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                    )
                }
                AyahButton(text = "▶", variant = AyahButtonVariant.Outline, onClick = onStart)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Browser materi ----
        AyahText(
            strings.lughohMaterialTitle,
            style = AyahTypography.Heading2,
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (state.levels.isEmpty()) {
            AyahText(
                strings.lughohEmpty,
                style = AyahTypography.Body1.copy(color = AyahColors.TextSecondary),
            )
        } else {
            state.levels.forEach { level ->
                LevelSection(
                    titleId = textOf(language, level.titleId, level.titleEn),
                    titleAr = level.titleAr,
                    lessons = level.lessons,
                    language = language,
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
    lessons: List<LughohLessonUi>,
    language: AppLanguage,
    onOpenLesson: (String) -> Unit,
) {
    AyahCard {
        Column {
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
            Spacer(modifier = Modifier.height(8.dp))
            lessons.forEach { lesson ->
                LessonRow(
                    lesson = lesson,
                    language = language,
                    onClick = { onOpenLesson(lesson.id) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LughohLessonUi,
    language: AppLanguage,
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
            "📖",
            style = AyahTypography.Body1,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            AyahText(
                textOf(language, lesson.titleId, lesson.titleEn),
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
// Detail materi pelajaran: Muhadatsah, Mufrodat, Qawa'id
// ---------------------------------------------------------------------------

@Composable
private fun LughohLessonView(
    state: LughohUiState,
    strings: Strings,
    onBack: () -> Unit,
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
                    textOf(state.language, lesson.titleId, lesson.titleEn),
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
                DialogueRow(line = line, language = state.language, arabicFamily = arabicFamily)
                if (index < lesson.muhadatsah.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Mufrodat
        SectionHeader(strings.lughohSectionMufrodat)
        lesson.mufrodat.forEach { word ->
            VocabCard(word = word, language = state.language, arabicFamily = arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Qawa'id
        SectionHeader(strings.lughohSectionQawaid)
        lesson.qawaid.forEach { rule ->
            GrammarCard(rule = rule, language = state.language, arabicFamily = arabicFamily)
            Spacer(modifier = Modifier.height(8.dp))
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
private fun DialogueRow(line: DialogueLine, language: AppLanguage, arabicFamily: FontFamily) {
    Column {
        AyahText(
            textOf(language, line.speaker, line.speakerEn),
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
            textOf(language, line.id, line.en),
            style = AyahTypography.Body2,
        )
    }
}

@Composable
private fun VocabCard(word: VocabWord, language: AppLanguage, arabicFamily: FontFamily) {
    AyahCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AyahText(
                    textOf(language, word.id, word.en),
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
            "— ${textOf(language, word.exampleId, word.exampleEn)}",
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
private fun GrammarCard(rule: GrammarRule, language: AppLanguage, arabicFamily: FontFamily) {
    AyahCard {
        AyahText(
            textOf(language, rule.titleId, rule.titleEn),
            style = AyahTypography.Body1.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            textOf(language, rule.id, rule.en),
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
            textOf(language, rule.exampleId, rule.exampleEn),
            style = AyahTypography.Caption,
        )
    }
}

// ---------------------------------------------------------------------------
// Sesi latihan acak (tadribat)
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
    onBackToHome: () -> Unit,
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
            ExerciseResult(state = state, strings = strings, onRestart = onRestart, onBackToHome = onBackToHome)
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

/** Hasil sesi latihan: skor + rekor + aksi ulangi / kembali. */
@Composable
private fun ExerciseResult(
    state: LughohUiState,
    strings: Strings,
    onRestart: () -> Unit,
    onBackToHome: () -> Unit,
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
            strings.lughohBestScore.format(state.stats.bestScore, state.total),
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
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
            text = strings.lughohBackToHome,
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}
