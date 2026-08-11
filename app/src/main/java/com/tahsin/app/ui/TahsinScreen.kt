package com.tahsin.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.stt.WordStatus
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahErrorView
import com.tahsin.app.ui.components.AyahLoadingView
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.ui.components.DropdownOption
import com.tahsin.app.ui.components.SimpleDropdown

/**
 * Layar utama Tahsin Quran: pilih surah → baca ayat ke mic → highlight
 * kata di mushaf (hijau benar / merah salah / kuning sedang dibaca),
 * daftar kesalahan + hukum tajwid, dan audio contoh.
 */
@Composable
fun TahsinScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: TahsinViewModel = viewModel(factory = tahsinViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.toggleMic()
        } else {
            viewModel.showMessage("Izin mikrofon diperlukan untuk mendeteksi bacaan.")
        }
    }

    when (val state = uiState) {
        TahsinUiState.Loading -> AyahLoadingView(modifier = modifier, message = "Memuat mushaf…")
        is TahsinUiState.Error -> AyahErrorView(
            message = state.message,
            onRetry = viewModel::reload,
            modifier = modifier,
        )
        is TahsinUiState.Ready -> TahsinContent(
            state = state,
            onSelectSurah = viewModel::selectSurah,
            onSelectAyah = viewModel::selectAyah,
            onPrevAyah = viewModel::prevAyah,
            onNextAyah = viewModel::nextAyah,
            onSelectWord = viewModel::selectWord,
            onMicClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.toggleMic() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onPlaySelectedWord = viewModel::playSelectedWord,
            onPlayAyah = viewModel::playAyah,
            onDismissMessage = viewModel::clearMessage,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TahsinContent(
    state: TahsinUiState.Ready,
    onSelectSurah: (Int) -> Unit,
    onSelectAyah: (Int) -> Unit,
    onPrevAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onSelectWord: (Int) -> Unit,
    onMicClick: () -> Unit,
    onPlaySelectedWord: () -> Unit,
    onPlayAyah: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ayah = state.ayah
    val words = ayah?.words.orEmpty()
    val statusByIndex = state.alignedWords.associateBy { it.index }
    val ayahCount = state.surah?.ayahs?.size ?: 0

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Header + legenda ----
        AyahText("Tahsin Quran", style = AyahTypography.Heading1)
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            "Baca ayat ke mikrofon — setiap kata dinilai real-time.",
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(color = AyahColors.Success, label = "benar")
            LegendDot(color = AyahColors.Error, label = "salah")
            LegendDot(color = AyahColors.Reading, label = "dibaca")
            LegendDot(color = AyahColors.Surface, label = "belum")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Pilih surah (dropdown, bukan tab) ----
        SimpleDropdown(
            selectedLabel = state.surah?.let { "${it.number}. ${it.nameLatin}" } ?: "-",
            options = state.surahs.map { s ->
                DropdownOption(
                    "${s.number}. ${s.nameLatin} (${s.ayahs.size} ayat)",
                    { onSelectSurah(s.number) },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Navigasi ayat ----
        if (ayah != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahButton(text = "‹", variant = AyahButtonVariant.Outline, onClick = onPrevAyah)
                Spacer(modifier = Modifier.width(12.dp))
                SimpleDropdown(
                    selectedLabel = "Ayat ${ayah.number} / $ayahCount",
                    options = (1..ayahCount).map { n ->
                        DropdownOption("Ayat $n", { onSelectAyah(n - 1) })
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                AyahButton(text = "›", variant = AyahButtonVariant.Outline, onClick = onNextAyah)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Mushaf: kata per kata dengan highlight (susunan RTL) ----
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        words.forEachIndexed { index, word ->
                            WordChip(
                                word = word,
                                status = statusByIndex[index]?.status,
                                selected = state.selectedWordIndex == index,
                                onClick = { onSelectWord(index) },
                            )
                        }
                    }
                }
            }
        }

        // ---- Transkrip real-time ----
        if (state.listening) {
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "🎙️ Terdeteksi: ${state.transcript.ifBlank { "…" }}",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Panel detail kata yang diketuk ----
        if (state.selectedWordIndex != null) {
            SelectedWordPanel(
                word = words.getOrNull(state.selectedWordIndex).orEmpty(),
                rules = state.selectedWordRules,
                onPlay = onPlaySelectedWord,
                onDismiss = { onSelectWord(-1) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---- Daftar kesalahan bacaan ----
        if (state.issues.isNotEmpty()) {
            AyahText(
                "Kesalahan terdeteksi (${state.issues.size})",
                style = AyahTypography.Heading2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.issues.forEach { issue ->
                IssueCard(issue = issue)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- Pesan sistem ----
        state.message?.let { msg ->
            AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onDismissMessage) {
                AyahText(msg, style = AyahTypography.Body2, color = AyahColors.Error)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ---- Bar bawah TETAP: mic + dengar ayat (tidak ikut scroll) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AyahColors.Background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicButton(listening = state.listening, onClick = onMicClick)
                Spacer(modifier = Modifier.width(14.dp))
                AyahText(
                    if (state.listening) "Membaca… tekan ⏹ untuk berhenti"
                    else "Tekan 🎙️ lalu bacalah ayat ini",
                    style = AyahTypography.Caption,
                    modifier = Modifier.weight(1f),
                )
                AyahButton(
                    text = "▶ Dengar Ayat",
                    variant = AyahButtonVariant.Secondary,
                    onClick = onPlayAyah,
                )
            }
        }
    }
}

// ============================================================ Sub-komponen

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .then(if (color == AyahColors.Surface) Modifier.border(1.dp, AyahColors.Divider) else Modifier),
        )
        Spacer(modifier = Modifier.width(4.dp))
        AyahText(label, style = AyahTypography.Caption)
    }
}

@Composable
private fun WordChip(
    word: String,
    status: WordStatus?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (bg, fg) = when (status) {
        WordStatus.CORRECT -> AyahColors.Success to Color.White
        WordStatus.MISMATCH -> AyahColors.Error to Color.White
        WordStatus.READING -> AyahColors.Reading to AyahColors.OnReading
        WordStatus.SKIPPED -> AyahColors.Error.copy(alpha = 0.18f) to AyahColors.TextSecondary
        WordStatus.NOT_REACHED -> AyahColors.Surface to AyahColors.TextPrimary
        null -> AyahColors.Surface to AyahColors.TextPrimary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (status == null) Modifier.border(1.dp, AyahColors.Divider)
                else Modifier,
            )
            .then(
                if (selected) Modifier.border(2.dp, AyahColors.Primary)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        AyahText(
            word,
            style = AyahTypography.ArabicWord.copy(
                color = fg,
                fontSize = 18.sp,
            ),
        )
    }
}

@Composable
private fun SelectedWordPanel(
    word: String,
    rules: List<com.tahsin.app.data.tajwid.TajwidRule>,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(word, style = AyahTypography.Arabic, modifier = Modifier.weight(1f))
            AyahButton(text = "▶ Kata", variant = AyahButtonVariant.Secondary, onClick = onPlay)
            Spacer(modifier = Modifier.width(8.dp))
            AyahButton(text = "✕", variant = AyahButtonVariant.Outline, onClick = onDismiss)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (rules.isEmpty()) {
            AyahText(
                "Tidak ada aturan tajwid khusus terdeteksi pada kata ini.",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        } else {
            rules.forEach { rule ->
                AyahText(
                    "• ${rule.name}",
                    style = AyahTypography.Body2.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                AyahText(rule.explanation, style = AyahTypography.Body2)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun IssueCard(issue: ReadingIssue) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText("🔴", style = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                issue.word,
                style = AyahTypography.ArabicWord.copy(fontSize = 20.sp),
                modifier = Modifier.weight(1f),
            )
        }
        issue.spoken?.let { spoken ->
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                "Terdeteksi: $spoken",
                style = AyahTypography.Caption.copy(color = AyahColors.Error),
            )
        }
        if (issue.rules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            issue.rules.forEach { rule ->
                AyahText(
                    "• ${rule.name} — ${rule.explanation}",
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                "Bacalah kata ini sesuai teks mushaf (huruf & harakat).",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        }
    }
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (listening) AyahColors.Error else AyahColors.Primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AyahText(
            if (listening) "⏹" else "🎙️",
            style = TextStyle(fontSize = 34.sp, color = Color.White),
        )
    }
}
