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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.stt.WordStatus
import com.tahsin.app.theme.ArabicFont
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
fun TahsinScreen(
    onOpenAudioManager: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
            onIncreaseFont = viewModel::increaseFont,
            onDecreaseFont = viewModel::decreaseFont,
            onSelectFont = viewModel::selectFont,
            onToggleDarkMode = viewModel::toggleDarkMode,
            onConfirmDownload = viewModel::confirmDownloadSurah,
            onOpenAudioManager = onOpenAudioManager,
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
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
    onSelectFont: (ArabicFont) -> Unit,
    onToggleDarkMode: () -> Unit,
    onConfirmDownload: (Boolean) -> Unit,
    onOpenAudioManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ayah = state.ayah
    val words = ayah?.words.orEmpty()
    val statusByIndex = state.alignedWords.associateBy { it.index }
    val ayahCount = state.surah?.ayahs?.size ?: 0

    var drawerOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Header + tombol hamburger (buka drawer pengaturan) ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                "Tahsin Quran",
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
            AyahButton(
                text = "☰",
                variant = AyahButtonVariant.Outline,
                onClick = { drawerOpen = true },
            )
        }
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
            selectedLabel = state.surah?.let { "${it.number}. ${it.nameLatin} (${it.ayahCount} ayat)" } ?: "-",
            options = state.surahs.map { s ->
                DropdownOption(
                    "${s.number}. ${s.nameLatin} (${s.ayahCount} ayat)",
                    { onSelectSurah(s.number) },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Navigasi ayat ----
        if (state.loadingSurah) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    "Memuat surah…",
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else if (ayah != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dalam arah baca Arab: tombol KIRI = lanjut ke ayat berikutnya.
                AyahButton(text = "‹", variant = AyahButtonVariant.Outline, onClick = onNextAyah)
                Spacer(modifier = Modifier.width(12.dp))
                SimpleDropdown(
                    selectedLabel = "Ayat ${ayah.number} / $ayahCount",
                    options = (1..ayahCount).map { n ->
                        DropdownOption("Ayat $n", { onSelectAyah(n - 1) })
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                AyahButton(text = "›", variant = AyahButtonVariant.Outline, onClick = onPrevAyah)
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
                                fontScale = state.fontScale,
                                fontFamily = state.arabicFont.family,
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
                fontScale = state.fontScale,
                fontFamily = state.arabicFont.family,
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
                IssueCard(
                    issue = issue,
                    fontScale = state.fontScale,
                    fontFamily = state.arabicFont.family,
                )
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

        // ---- Drawer kanan: pengaturan (ukuran font, jenis font, tema) ----
        if (drawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { drawerOpen = false },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(300.dp)
                    .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(AyahColors.Surface)
                    .padding(20.dp),
            ) {
                SettingsPanel(
                    state = state,
                    onDecreaseFont = onDecreaseFont,
                    onIncreaseFont = onIncreaseFont,
                    onSelectFont = onSelectFont,
                    onToggleDarkMode = onToggleDarkMode,
                    onOpenAudioManager = {
                        drawerOpen = false
                        onOpenAudioManager()
                    },
                    onClose = { drawerOpen = false },
                )
            }
        }

        // ---- Dialog konfirmasi unduh audio surah ----
        state.confirmDownloadSurah?.let { number ->
            val surah = state.surahs.find { it.number == number }
            ConfirmDownloadDialog(
                surahLabel = surah?.let { "${it.number}. ${it.nameLatin} (${it.ayahCount} ayat)" }
                    ?: "surah $number",
                onConfirm = { onConfirmDownload(true) },
                onCancel = { onConfirmDownload(false) },
            )
        }

        // ---- Dialog progress unduh ----
        if (state.isDownloading) {
            DownloadProgressDialog(
                surahLabel = state.downloadingSurah
                    ?.let { n -> state.surahs.find { it.number == n }?.nameLatin }
                    .orEmpty(),
                done = state.downloadProgress?.first ?: 0,
                total = state.downloadProgress?.second ?: 0,
            )
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
    fontScale: Float,
    fontFamily: FontFamily,
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
                fontSize = (18 * fontScale).sp,
                fontFamily = fontFamily,
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
    fontScale: Float,
    fontFamily: FontFamily,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                word,
                style = AyahTypography.Arabic.copy(
                    fontSize = (26 * fontScale).sp,
                    fontFamily = fontFamily,
                ),
                modifier = Modifier.weight(1f),
            )
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
private fun IssueCard(
    issue: ReadingIssue,
    fontScale: Float,
    fontFamily: FontFamily,
) {
    AyahCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText("🔴", style = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                issue.word,
                style = AyahTypography.ArabicWord.copy(
                    fontSize = (20 * fontScale).sp,
                    fontFamily = fontFamily,
                ),
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

// ============================================================ Drawer pengaturan

@Composable
private fun SettingsPanel(
    state: TahsinUiState.Ready,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onSelectFont: (ArabicFont) -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenAudioManager: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                "Pengaturan",
                style = AyahTypography.Heading2,
                modifier = Modifier.weight(1f),
            )
            AyahButton(text = "✕", variant = AyahButtonVariant.Outline, onClick = onClose)
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Ukuran teks Arab")
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(
                text = "A−",
                variant = AyahButtonVariant.Outline,
                onClick = onDecreaseFont,
                enabled = state.fontScale > 1.01f,
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                "${(state.fontScale * 100).toInt()}%",
                style = AyahTypography.Caption,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahButton(
                text = "A+",
                variant = AyahButtonVariant.Outline,
                onClick = onIncreaseFont,
                enabled = state.fontScale < 1.49f,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Jenis font")
        Spacer(modifier = Modifier.height(8.dp))
        SimpleDropdown(
            selectedLabel = state.arabicFont.label,
            options = ArabicFont.entries.map { f ->
                DropdownOption(f.label, { onSelectFont(f) })
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Tema")
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = if (state.darkMode) "☀️ Mode Terang" else "🌙 Mode Gelap",
            variant = if (state.darkMode) AyahButtonVariant.Secondary else AyahButtonVariant.Primary,
            onClick = onToggleDarkMode,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Penyimpanan")
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = "🎵 Kelola audio terunduh",
            variant = AyahButtonVariant.Outline,
            onClick = onOpenAudioManager,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(28.dp))

        AyahText(
            "Jenis font mushaf (Amiri Quran, Scheherazade New) bisa " +
                "ditambahkan lewat res/font pada versi berikutnya.",
            style = AyahTypography.Caption,
        )

        Spacer(modifier = Modifier.weight(1f))

        AyahText(
            "Ketuk di luar drawer untuk menutup.",
            style = AyahTypography.Caption,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    AyahText(
        text,
        style = AyahTypography.Body2.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

// ============================================================ Dialog

/** Lapisan scrim + konten dialog di tengah layar. */
@Composable
private fun DialogScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Popup konfirmasi unduh audio satu surah. */
@Composable
private fun ConfirmDownloadDialog(
    surahLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    DialogScrim {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText("Unduh audio surah ini?", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "Audio Minshawy (per ayat) + audio kata-kata untuk $surahLabel " +
                    "akan diunduh ke penyimpanan aplikasi supaya bisa diputar offline.",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(text = "Batal", variant = AyahButtonVariant.Outline, onClick = onCancel)
                Spacer(modifier = Modifier.width(12.dp))
                AyahButton(text = "📥 Unduh", variant = AyahButtonVariant.Primary, onClick = onConfirm)
            }
        }
    }
}

/** Dialog progress unduh audio satu surah. */
@Composable
private fun DownloadProgressDialog(
    surahLabel: String,
    done: Int,
    total: Int,
) {
    DialogScrim {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText("Mengunduh audio…", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(4.dp))
            if (surahLabel.isNotBlank()) {
                AyahText(surahLabel, style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary))
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProgressBar(fraction = if (total > 0) done.toFloat() / total else 0f)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "$done / $total",
                style = AyahTypography.Caption,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Progress bar kustom sederhana (tanpa Material 3). */
@Composable
private fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(AyahColors.Divider),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(AyahColors.Primary),
        )
    }
}
