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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.data.tajwid.RuleCategory
import com.tahsin.app.data.tajwid.TajwidColorizer
import com.tahsin.app.data.tajwid.TajwidEngine
import com.tahsin.app.data.tajwid.TajwidRule
import com.tahsin.app.data.tajwid.TajwidSpan
import com.tahsin.app.stt.WordStatus
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.ui.components.AyahButton
import com.tahsin.app.ui.components.AyahButtonSize
import com.tahsin.app.ui.components.AyahButtonVariant
import com.tahsin.app.ui.components.AyahCard
import com.tahsin.app.ui.components.AyahErrorView
import com.tahsin.app.ui.components.AyahLoadingView
import com.tahsin.app.ui.components.AyahText
import com.tahsin.app.ui.components.DropdownOption
import com.tahsin.app.ui.components.SimpleDropdown
import kotlin.math.roundToInt

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
            onDismissMessage = viewModel::clearMessage,
            onToggleDarkMode = viewModel::toggleDarkMode,
            onToggleTajwidColor = viewModel::toggleTajwidColor,
            onToggleFlowMode = viewModel::toggleFlowMode,
            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
            onDismissDownloadNotice = viewModel::dismissDownloadNotice,
            onDownloadAll = viewModel::downloadAllAudio,
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
    onDismissMessage: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleTajwidColor: () -> Unit,
    onToggleFlowMode: () -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onDismissDownloadNotice: () -> Unit,
    onDownloadAll: () -> Unit,
    onOpenAudioManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ayah = state.ayah
    val words = ayah?.words.orEmpty()
    val statusByIndex = state.alignedWords.associateBy { it.index }
    val ayahCount = state.surah?.ayahs?.size ?: 0

    // Span warna tajwid per kata (dihitung sekali per ayat).
    val spansByWord = remember(words) {
        words.mapIndexed { idx, w ->
            TajwidColorizer.spans(
                w,
                TajwidEngine.analyzeWord(w, words.getOrNull(idx - 1), words.getOrNull(idx + 1)),
            )
        }
    }

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
                "Tahsin Qur'an",
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
            AyahButton(
                text = if (state.darkMode) "☀️" else "🌙",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onToggleDarkMode,
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahButton(
                text = "⚙",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = { drawerOpen = true },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            "Baca ayat ke mikrofon.\nSetiap kata dicek langsung.",
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
                AyahButton(
                    text = "‹",
                    variant = AyahButtonVariant.Outline,
                    size = AyahButtonSize.Small,
                    onClick = onNextAyah,
                )
                Spacer(modifier = Modifier.width(10.dp))
                SimpleDropdown(
                    selectedLabel = "Ayat ${ayah.number} / $ayahCount",
                    options = (1..ayahCount).map { n ->
                        DropdownOption("Ayat $n", { onSelectAyah(n - 1) })
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AyahButton(
                    text = "›",
                    variant = AyahButtonVariant.Outline,
                    size = AyahButtonSize.Small,
                    onClick = onPrevAyah,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Mushaf: kata per kata dengan highlight (susunan RTL) ----
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val maxWordPx = with(density) { (maxWidth - 24.dp).toPx() }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            words.forEachIndexed { index, word ->
                                val selected = state.selectedWordIndex == index
                                // Tooltip (overlay) saat dipilih — struktur ayat tidak berubah.
                                WordChipWithTooltip(
                                    word = word,
                                    status = statusByIndex[index]?.status,
                                    selected = selected,
                                    onClick = { onSelectWord(if (selected) -1 else index) },
                                    fontScale = state.fontScale,
                                    fontFamily = state.arabicFontFamily,
                                    tajwidColor = state.tajwidColor,
                                    spans = spansByWord[index],
                                    maxWordWidthPx = maxWordPx,
                                    rules = state.selectedWordRules,
                                    onPlayWord = onPlaySelectedWord,
                                    onDismiss = { onSelectWord(-1) },
                                )
                            }
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
                    fontFamily = state.arabicFontFamily,
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

            // ---- Progress unduh audio (di ATAS tombol mic & dengar) ----
            DownloadFooter(
                isDownloading = state.isDownloading,
                done = state.downloadDone,
                total = state.downloadTotal,
            )

            // ---- Bar bawah TETAP: mic + dengar/stop (tidak ikut scroll) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AyahColors.Background)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MicButton(listening = state.listening, onClick = onMicClick)
                    Spacer(modifier = Modifier.width(14.dp))
                    AyahText(
                        if (state.listening) "Membaca… tekan ⏹ untuk berhenti"
                        else "Tekan 🎙️\nlalu bacalah",
                        style = AyahTypography.Caption,
                        modifier = Modifier.weight(1f),
                    )
                    AyahButton(
                        text = if (state.isAudioPlaying) "⏹ Stop" else "▶ Dengar",
                        variant = if (state.isAudioPlaying) AyahButtonVariant.Danger else AyahButtonVariant.Secondary,
                        onClick = onToggleAudioPlayback,
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
                    onToggleTajwidColor = onToggleTajwidColor,
                    onToggleFlowMode = onToggleFlowMode,
                    onDownloadAll = onDownloadAll,
                    onOpenAudioManager = {
                        drawerOpen = false
                        onOpenAudioManager()
                    },
                    onClose = { drawerOpen = false },
                )
            }
        }

        // ---- Popup keterangan: unduh audio dimulai ----
        if (state.showDownloadNotice) {
            DownloadNoticeDialog(onDismiss = onDismissDownloadNotice)
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
                .background(color, CircleShape)
                .then(
                    if (color == AyahColors.Surface) Modifier.border(1.dp, AyahColors.Divider, CircleShape)
                    else Modifier,
                ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        AyahText(label, style = AyahTypography.Caption)
    }
}

/** Tooltip mengambang untuk kata yang dipilih: play + keterangan tajwid. */
@Composable
private fun WordChipWithTooltip(
    word: String,
    status: WordStatus?,
    selected: Boolean,
    onClick: () -> Unit,
    fontScale: Float,
    fontFamily: FontFamily,
    tajwidColor: Boolean,
    spans: List<TajwidSpan>,
    maxWordWidthPx: Float,
    rules: List<TajwidRule>,
    onPlayWord: () -> Unit,
    onDismiss: () -> Unit,
) {
    var chipHeightPx by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier.onSizeChanged { chipHeightPx = it.height },
    ) {
        WordChip(
            word = word,
            status = status,
            selected = selected,
            onClick = onClick,
            fontScale = fontScale,
            fontFamily = fontFamily,
            tajwidColor = tajwidColor,
            spans = spans,
            maxWordWidthPx = maxWordWidthPx,
        )
        if (selected) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, chipHeightPx + 6),
                onDismissRequest = onDismiss,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 220.dp, max = 320.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(AyahColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, AyahColors.Hairline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AyahText(
                                word,
                                style = AyahTypography.Arabic.copy(
                                    fontSize = 20.sp,
                                    fontFamily = fontFamily,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AyahButton(
                                text = "▶ Kata",
                                variant = AyahButtonVariant.Secondary,
                                size = AyahButtonSize.Small,
                                onClick = onPlayWord,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (rules.isEmpty()) {
                            AyahText(
                                "Tidak ada aturan tajwid khusus pada kata ini.",
                                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                            )
                        } else {
                            rules.forEach { r ->
                                AyahText(
                                    "• ${r.name} — ${r.explanation}",
                                    style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                                )
                            }
                        }
                    }
                }
            }
        }
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
    tajwidColor: Boolean,
    spans: List<TajwidSpan>,
    maxWordWidthPx: Float,
) {
    val shape = RoundedCornerShape(10.dp)
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
            .background(bg, shape)
            .then(
                if (status == null) Modifier.border(1.dp, AyahColors.Divider, shape)
                else Modifier,
            )
            .then(
                if (selected) Modifier.border(2.dp, AyahColors.Primary, shape)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // Pada chip berwarna penuh (benar/salah/dibaca/terlewat) warna tajwid
        // dimatikan agar tetap terbaca; pada chip netral ditampilkan.
        val plainBg = status == null || status == WordStatus.NOT_REACHED
        val visibleSpans = if (tajwidColor && plainBg) spans else emptyList()
        val baseStyle = AyahTypography.ArabicWord.copy(
            color = fg,
            fontSize = (18 * fontScale).sp,
            fontFamily = fontFamily,
        )
        val style = fitArabicStyle(word, visibleSpans, baseStyle, maxWordWidthPx)
        AyahText(
            tajwidAnnotated(word, visibleSpans),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
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
    val base = if (listening) AyahColors.Error else AyahColors.Primary
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(base, CircleShape)
            .border(4.dp, base.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AyahText(
            if (listening) "⏹" else "🎙️",
            style = TextStyle(fontSize = 24.sp, color = Color.White),
        )
    }
}

// ============================================================ Drawer pengaturan

@Composable
private fun SettingsPanel(
    state: TahsinUiState.Ready,
    onToggleTajwidColor: () -> Unit,
    onToggleFlowMode: () -> Unit,
    onDownloadAll: () -> Unit,
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

        SectionLabel("Tajwid")
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = if (state.tajwidColor) "🎨 Warna Tajwid: Nyala" else "🎨 Warna Tajwid: Mati",
            variant = if (state.tajwidColor) AyahButtonVariant.Secondary else AyahButtonVariant.Outline,
            onClick = onToggleTajwidColor,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Muroja'ah")
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = if (state.flowMode) "🔁 Mode Flow: Nyala" else "🔁 Mode Flow: Mati",
            variant = if (state.flowMode) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
            onClick = onToggleFlowMode,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AyahText(
            if (state.flowMode) "Lanjut otomatis ke ayat berikutnya saat semua kata benar."
            else "Lanjut otomatis antar-ayat untuk muroja'ah.",
            style = AyahTypography.Caption,
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel("Penyimpanan")
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = "Unduh semua audio",
            variant = AyahButtonVariant.Primary,
            onClick = onDownloadAll,
            enabled = !state.isDownloading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AyahButton(
            text = "Kelola audio terunduh",
            variant = AyahButtonVariant.Outline,
            onClick = onOpenAudioManager,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Spacer(modifier = Modifier.weight(1f))

        AyahText(
            "Ketuk di luar panel untuk menutup.",
            style = AyahTypography.Caption,
        )

        Spacer(modifier = Modifier.height(12.dp))

        AyahText(
            "Made with ❤️ by Lutfian Dwi Cahyono",
            style = AyahTypography.Caption.copy(
                color = AyahColors.Primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    AyahText(
        text,
        style = AyahTypography.Overline.copy(
            color = AyahColors.Primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

// ============================================================ Footer unduh

/** Footer progress unduh audio (di paling bawah, tidak nge-block view). */
@Composable
private fun DownloadFooter(
    isDownloading: Boolean,
    done: Int,
    total: Int,
) {
    if (!isDownloading) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AyahColors.Background)
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                "Mengunduh audio…",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                modifier = Modifier.weight(1f),
            )
            AyahText(
                "$done / $total",
                style = AyahTypography.Caption,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ProgressBar(fraction = if (total > 0) done.toFloat() / total else 0f)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Popup keterangan singkat saat unduhan audio dimulai. */
@Composable
private fun DownloadNoticeDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(AyahColors.Surface)
                .padding(20.dp),
        ) {
            AyahText("Mengunduh audio…", style = AyahTypography.Heading2)
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "Audio surah ini belum diunduh. Unduhan dimulai otomatis — " +
                    "ikuti progres di atas tombol. Bacaan contoh akan diputar " +
                    "setelah unduhan selesai.",
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                AyahButton(text = "Mengerti", variant = AyahButtonVariant.Primary, onClick = onDismiss)
            }
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

// ============================================================ Warna tajwid

/** Warna huruf untuk satu kategori hukum tajwid (null = tidak diwarnai). */
private fun tajwidColorFor(category: RuleCategory): Color? = when (category) {
    RuleCategory.MAD -> AyahColors.TajwidMad
    RuleCategory.GHUNNAH -> AyahColors.TajwidGhunnah
    RuleCategory.QALQALAH -> AyahColors.TajwidQalqalah
    RuleCategory.IKHFA -> AyahColors.TajwidIkhfa
    RuleCategory.IQLAB -> AyahColors.TajwidIqlab
    RuleCategory.IDGHAM -> AyahColors.TajwidIdgham
    RuleCategory.LAM_JALALAH -> AyahColors.TajwidLamJalalah
    else -> null
}

/** Teks Arab dengan span warna tajwid (hanya huruf yang kena hukum). */
private fun tajwidAnnotated(word: String, spans: List<TajwidSpan>): AnnotatedString {
    if (spans.isEmpty()) return AnnotatedString(word)
    return buildAnnotatedString {
        append(word)
        for (sp in spans) {
            val color = tajwidColorFor(sp.category) ?: continue
            addStyle(SpanStyle(color = color), sp.start, sp.end.coerceAtMost(word.length))
        }
    }
}

/**
 * Auto-fit teks Arab: ukur lebar kata pada ukuran dasar; kalau melebihi
 * `maxWidthPx`, kecilkan font secara proporsional supaya SELURUH kata selalu
 * muat satu baris (huruf tidak pernah putus ke baris bawah / terpotong).
 */
@Composable
private fun fitArabicStyle(
    text: String,
    spans: List<TajwidSpan>,
    baseStyle: TextStyle,
    maxWidthPx: Float,
): TextStyle {
    val textMeasurer = rememberTextMeasurer()
    val annotated = remember(text, spans) { tajwidAnnotated(text, spans) }
    val baseSizeSp = baseStyle.fontSize.value
    val fontFamily = baseStyle.fontFamily
    return remember(text, spans, baseSizeSp, fontFamily, maxWidthPx) {
        val maxW = maxWidthPx.roundToInt().coerceAtLeast(1)
        // Ukur TANPA batas lebar agar mendapat lebar intrinsik satu baris —
        // kalau diukur dengan maxWidth, teks panjang wrap dan hasilnya selalu
        // <= maxW, sehingga auto-fit tidak pernah jalan (sumber bug kepotong).
        val layout = textMeasurer.measure(annotated, baseStyle)
        if (layout.size.width <= maxW) {
            baseStyle
        } else {
            val scale = maxW.toFloat() / layout.size.width
            val fittedSp = baseSizeSp * scale
            baseStyle.copy(
                fontSize = if (fittedSp >= 10f) fittedSp.sp else 10.sp,
            )
        }
    }
}
