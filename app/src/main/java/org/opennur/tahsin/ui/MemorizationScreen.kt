package org.opennur.tahsin.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.learning.MemorizationCard
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.stt.WordStatus
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.DropdownOption
import org.opennur.tahsin.ui.components.SimpleDropdown

@Composable
fun MemorizationScreen(
    onBack: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MemorizationViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = AppStrings.of(state.language)
    LaunchedEffect(viewModel) { viewModel.refreshLanguage() }
    LaunchedEffect(Unit) { viewModel.checkMicPermission() }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.checkMicPermission()
        if (granted) viewModel.toggleMic()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // ---- Header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(text = "\u2190", variant = AyahButtonVariant.Outline, onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            AyahText(
                strings.memorizationTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.memorizationSubtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ---- Target picker ----
        MemorizationTargetPicker(state, strings, viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.loading -> AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(strings.memorizationLoading, style = AyahTypography.Body2)
            }
            state.error || state.card == null || state.ayah == null -> AyahCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AyahText(strings.memorizationError, style = AyahTypography.Body2)
            }
            else -> MemorizationReadyContent(
                state, strings, viewModel, onOpenAyah,
                onMicClick = {
                    if (state.hasMicPermission) viewModel.toggleMic()
                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemorizationTargetPicker(
    state: MemorizationUiState,
    strings: Strings,
    viewModel: MemorizationViewModel,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(
                if (state.targetMode == "surah") strings.memorizationTargetSurah
                else strings.memorizationTargetJuz,
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Mode toggle
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AyahButton(
                    text = strings.memorizationTargetSurah,
                    variant = if (state.targetMode == "surah") AyahButtonVariant.Primary
                    else AyahButtonVariant.Outline,
                    size = AyahButtonSize.Small,
                    onClick = { viewModel.setTargetMode("surah") },
                )
                AyahButton(
                    text = strings.memorizationTargetJuz,
                    variant = if (state.targetMode == "juz") AyahButtonVariant.Primary
                    else AyahButtonVariant.Outline,
                    size = AyahButtonSize.Small,
                    onClick = { viewModel.setTargetMode("juz") },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Selector
            if (state.targetMode == "surah") {
                val selected = state.availableSurahs.firstOrNull { it.number == state.selectedSurah }
                SimpleDropdown(
                    selectedLabel = selected?.let { "${it.number}. ${it.nameLatin}" } ?: "-",
                    options = state.availableSurahs.map { s ->
                        DropdownOption("${s.number}. ${s.nameLatin} (${s.ayahCount})") {
                            viewModel.selectSurah(s.number)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SimpleDropdown(
                    selectedLabel = "Juz ${state.selectedJuz}",
                    options = (1..30).map { j ->
                        DropdownOption("Juz $j") { viewModel.selectJuz(j) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AyahButton(
                text = strings.memorizationApply,
                variant = AyahButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.applyTarget() },
            )
        }
    }
}

@Composable
private fun MemorizationReadyContent(
    state: MemorizationUiState,
    strings: Strings,
    viewModel: MemorizationViewModel,
    onOpenAyah: (Int, Int) -> Unit,
    onMicClick: () -> Unit,
) {
    val card = state.card ?: return
    val ayah = state.ayah ?: return
    AyahText(
        strings.memorizationDue.format(state.dueCount, state.totalCount),
        style = AyahTypography.Caption.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    Spacer(modifier = Modifier.height(10.dp))
    MemorizationAyahCard(card, ayah, state, strings, viewModel::reveal)

    // ---- STT recitation section ----
    if (state.revealed) {
        Spacer(modifier = Modifier.height(12.dp))
        MemorizationSttSection(state, strings, onMicClick, viewModel::clearSttSession)
    }

    // ---- Remember / Review buttons ----
    if (state.revealed && state.sttScore == null) {
        Spacer(modifier = Modifier.height(12.dp))
        MemorizationAnswerButtons(strings, viewModel::needReview, viewModel::remember)
    }
    Spacer(modifier = Modifier.height(10.dp))
    AyahButton(
        text = strings.memorizationOpenTahsin,
        variant = AyahButtonVariant.Outline,
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenAyah(card.surah, card.ayah) },
    )
}

@Composable
private fun MemorizationAyahCard(
    card: MemorizationCard,
    ayah: Ayah,
    state: MemorizationUiState,
    strings: Strings,
    onReveal: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            AyahText(
                "${card.surah}:${card.ayah}",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (state.revealed) {
                if (state.alignedWords.isNotEmpty()) {
                    MemorizationSttText(ayah, state)
                } else {
                    AyahText(
                        ayah.text,
                        style = AyahTypography.Arabic.copy(textAlign = TextAlign.End),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                AyahText(
                    ayah.translation,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            } else {
                AyahText(
                    strings.memorizationHidden,
                    style = AyahTypography.Body1.copy(
                        color = AyahColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                AyahButton(
                    text = strings.memorizationReveal,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onReveal,
                )
            }
        }
    }
}

@Composable
private fun MemorizationSttText(
    ayah: Ayah,
    state: MemorizationUiState,
) {
    val words = ayah.words
    val aligned = state.alignedWords
    val annotated: AnnotatedString = buildAnnotatedString {
        for ((i, word) in words.withIndex()) {
            if (i > 0) append(" ")
            val match = aligned.firstOrNull { it.index == i }
            val bg = when (match?.status) {
                WordStatus.CORRECT -> AyahColors.Success.copy(alpha = 0.3f)
                WordStatus.MISMATCH -> AyahColors.Error.copy(alpha = 0.3f)
                WordStatus.READING -> AyahColors.Reading.copy(alpha = 0.5f)
                else -> null
            }
            if (bg != null) {
                withStyle(SpanStyle(background = bg)) { append(word) }
            } else {
                append(word)
            }
        }
    }
    AyahText(
        annotated,
        style = AyahTypography.Arabic.copy(textAlign = TextAlign.End),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MemorizationSttSection(
    state: MemorizationUiState,
    strings: Strings,
    onMicClick: () -> Unit,
    onClear: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (state.listening) {
                AyahText(
                    strings.memorizationSttListening,
                    style = AyahTypography.Body2.copy(color = AyahColors.Primary),
                )
            } else if (state.sttScore != null) {
                val score = state.sttScore
                val pass = score >= 80
                AyahText(
                    strings.memorizationSttScore.format(score),
                    style = AyahTypography.Heading2.copy(
                        color = if (pass) AyahColors.Success else AyahColors.Error,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AyahText(
                    if (pass) strings.memorizationSttPass else strings.memorizationSttFail,
                    style = AyahTypography.Body2.copy(
                        color = if (pass) AyahColors.Success else AyahColors.Error,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    AyahButton(
                        text = strings.memorizationRecite,
                        variant = AyahButtonVariant.Secondary,
                        size = AyahButtonSize.Small,
                        onClick = onClear,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (pass) {
                        AyahButton(
                            text = strings.memorizationRemembered,
                            size = AyahButtonSize.Small,
                            onClick = {},
                        )
                    }
                }
            } else {
                AyahButton(
                    text = strings.memorizationRecite,
                    variant = AyahButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMicClick,
                )
            }
        }
    }
}

@Composable
private fun MemorizationAnswerButtons(
    strings: Strings,
    onReview: () -> Unit,
    onRemember: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AyahButton(
            text = strings.memorizationReview,
            variant = AyahButtonVariant.Danger,
            modifier = Modifier.weight(1f),
            onClick = onReview,
        )
        Spacer(modifier = Modifier.width(8.dp))
        AyahButton(
            text = strings.memorizationRemembered,
            modifier = Modifier.weight(1f),
            onClick = onRemember,
        )
    }
}
