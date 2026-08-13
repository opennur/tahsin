package com.tahsin.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tahsin.app.data.tajwid.RuleCategory
import com.tahsin.app.data.tajwid.TajwidColorizer
import com.tahsin.app.data.tajwid.TajwidEngine
import com.tahsin.app.data.tajwid.TajwidSpan
import com.tahsin.app.stt.AlignedWord
import com.tahsin.app.stt.WordStatus
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTypography
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.DownloadProgress
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
    /** ViewModel bersama (dibuat di MainActivity, scope activity). */
    viewModel: TahsinViewModel,
    /** Buka layar pencarian ayat (tombol 🔍 di header). */
    onOpenSearch: () -> Unit = {},
    /** Buka layar Pengaturan (ikon ⚙ di header). */
    onOpenSettings: () -> Unit = {},
    /** Kembali ke layar sebelumnya (tombol ← di header). */
    onBack: () -> Unit = {},
    /** Target buka dari widget/notifikasi "Ayah of the Day" (surah, ayat 1-based). */
    target: OpenTarget? = null,
    /** Dipanggil setelah [target] dikirim ke ViewModel — target hanya dipakai sekali. */
    onTargetConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Bahasa untuk pesan di luar TahsinContent (loading/error/izin mik).
    val strings = AppStrings.of(viewModel.settingsState.value.language)

    // Buka surah/ayat yang diminta widget/notifikasi. Key = OpenTarget (data class,
    // berisi deliveryId unik per pengiriman) sehingga ketukan widget yang sama
    // berulang tetap memicu LaunchedEffect. Target dikonsumsi sekali supaya tidak
    // terkirim ulang saat Tahsin dibuka lagi dari portal.
    LaunchedEffect(target) {
        target?.let {
            viewModel.openAt(it.surah, it.ayah)
            onTargetConsumed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.toggleMic()
        } else {
            viewModel.showMessage(strings.msgMicPermission)
        }
    }

    // Izin notifikasi (Android 13+) dipindah ke layar Pengaturan (toggle
    // "Ayah of the Day" kini berada di sana).
    when (val state = uiState) {
        TahsinUiState.Loading -> AyahLoadingView(modifier = modifier, message = strings.msgMushafLoading)
        is TahsinUiState.Error -> AyahErrorView(
            message = state.message,
            onRetry = viewModel::reload,
            retryLabel = strings.msgRetry,
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
            onStopSelectedWord = viewModel::stopWordPlayback,
            onDismissMessage = viewModel::clearMessage,
            onDismissSwipeHint = viewModel::dismissSwipeHint,
            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

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
    onStopSelectedWord: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismissSwipeHint: () -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ayah = state.ayah
    val words = ayah?.words.orEmpty()
    val statusByIndex = state.alignedWords.associateBy { it.index }
    val ayahCount = state.surah?.ayahs?.size ?: 0
    val strings = AppStrings.of(state.language)

    // Span warna tajwid per kata (dihitung sekali per ayat).
    val spansByWord = remember(words) {
        words.mapIndexed { idx, w ->
            TajwidColorizer.spans(
                w,
                TajwidEngine.analyzeWord(w, words.getOrNull(idx - 1), words.getOrNull(idx + 1)),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AyahColors.Background)) {
        // Inset system bars (status + nav) — konten aman dari underlap (edge-to-edge).
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    // SWIPE di SELURUH latar konten (background) untuk ganti ayat.
                    // Event yang sudah dikonsumsi (mushaf/scroll/dropdown) diabaikan
                    // supaya tidak dobel pindah & tidak mengganggu interaksi lain.
                    val swipeThreshold = 80.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var totalX = 0f
                        var totalY = 0f
                        var swiping = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (event.changes.any { it.isConsumed }) {
                                totalX = 0f
                                totalY = 0f
                                swiping = false
                            } else {
                                totalX += change.position.x - change.previousPosition.x
                                totalY += change.position.y - change.previousPosition.y
                            }
                            if (!swiping &&
                                kotlin.math.abs(totalX) > swipeThreshold &&
                                kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                            ) {
                                swiping = true
                            }
                            if (swiping) change.consume()
                        }
                        if (swiping) {
                            // RTL: geser kanan = ayat berikutnya, kiri = sebelumnya.
                            if (totalX >= swipeThreshold) onNextAyah()
                            else if (totalX <= -swipeThreshold) onPrevAyah()
                        }
                    }
                }
                .padding(horizontal = 20.dp),
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Header: kembali + judul + pencarian + pengaturan (⚙) ----
        // Mode gelap & bahasa kini di layar Pengaturan (bukan drawer).
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahButton(
                text = "←",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(
                strings.appTitle,
                style = AyahTypography.Heading1,
                modifier = Modifier.weight(1f),
            )
            AyahButton(
                text = "🔍",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onOpenSearch,
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahButton(
                text = "⚙",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onOpenSettings,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AyahText(
            strings.subtitle,
            style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(color = AyahColors.Success, label = strings.legendCorrect)
            LegendDot(color = AyahColors.Error, label = strings.legendWrong)
            LegendDot(color = AyahColors.Reading, label = strings.legendReading)
            LegendDot(color = AyahColors.Surface, label = strings.legendNotReached)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Navigasi satu baris: [prev] [surah ▾] [ayat ▾] [next] ----
        if (ayah != null && !state.loadingSurah) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahButton(
                    text = "‹",
                    variant = AyahButtonVariant.Outline,
                    size = AyahButtonSize.Small,
                    onClick = onNextAyah,
                )
                Spacer(modifier = Modifier.width(10.dp))
                SimpleDropdown(
                    selectedLabel = state.surah?.let { "${it.number}. ${it.nameLatin}" } ?: "-",
                    options = state.surahs.map { s ->
                        DropdownOption("${s.number}. ${s.nameLatin}", { onSelectSurah(s.number) })
                    },
                    modifier = Modifier.weight(2f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                SimpleDropdown(
                    selectedLabel = "${ayah.number}",
                    options = (1..ayahCount).map { n ->
                        DropdownOption("$n", { onSelectAyah(n - 1) })
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
            Spacer(modifier = Modifier.height(4.dp))
            // Petunjuk geser — bisa ditutup permanen lewat tombol ✕.
            if (state.showSwipeHint) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AyahText(
                        strings.swipeHint,
                        style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                        modifier = Modifier.weight(1f),
                    )
                    AyahButton(
                        text = "✕",
                        variant = AyahButtonVariant.Ghost,
                        size = AyahButtonSize.Small,
                        onClick = onDismissSwipeHint,
                    )
                }
            }
        } else {
            // Sedang memuat / belum ada ayat — dropdown surah tetap tersedia.
            SimpleDropdown(
                selectedLabel = state.surah?.let { "${it.number}. ${it.nameLatin}" } ?: "-",
                options = state.surahs.map { s ->
                    DropdownOption("${s.number}. ${s.nameLatin}", { onSelectSurah(s.number) })
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Muat konten surah / mushaf ----

        // ---- Navigasi ayat ----
        if (state.loadingSurah) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    strings.loadingSurah,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else if (ayah != null) {
            // ---- Mushaf kontinu (gaya mushaf asli: kata tersambung RTL) ----
            // Satu gesture menangani TAP (pilih kata → tooltip) dan SWIPE
            // (ganti ayat) sekaligus — tanpa konflik arena gesture.
            val density = LocalDensity.current
            val swipeThresholdPx = with(density) { 80.dp.toPx() }
            val tapSlopPx = with(density) { 12.dp.toPx() }
            // Mushaf + terjemahan + transkrip + kesalahan (layout berurutan).
            // Gesture swipe kini ada di level scroll Column (mencakup background),
            // jadi wrapper ini cukup untuk tata letak saja.
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AyahCard(modifier = Modifier.fillMaxWidth()) {
                var textLayout by remember(words) { mutableStateOf<TextLayoutResult?>(null) }
                val wordOffsets = remember(words) {
                    val arr = IntArray(words.size)
                    var pos = 0
                    words.forEachIndexed { i, w ->
                        arr[i] = pos
                        pos += w.length + 1 // kata + spasi
                    }
                    arr
                }
                val annotated = remember(words, spansByWord, statusByIndex, state.selectedWordIndex, state.tajwidColor) {
                    buildAyahAnnotated(
                        words = words,
                        spansByWord = spansByWord,
                        statusByIndex = statusByIndex,
                        selectedIndex = state.selectedWordIndex,
                        tajwidColor = state.tajwidColor,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(state.selectedWordIndex) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var lastPos = down.position
                                var totalX = 0f
                                var totalY = 0f
                                var swiping = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    lastPos = change.position
                                    totalX += change.position.x - change.previousPosition.x
                                    totalY += change.position.y - change.previousPosition.y
                                    if (!swiping &&
                                        kotlin.math.abs(totalX) > swipeThresholdPx &&
                                        kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                                    ) {
                                        swiping = true
                                    }
                                    if (swiping) change.consume()
                                }
                                if (swiping) {
                                    // RTL: geser kanan = ayat berikutnya, kiri = sebelumnya.
                                    if (totalX >= swipeThresholdPx) onNextAyah()
                                    else if (totalX <= -swipeThresholdPx) onPrevAyah()
                                } else if (kotlin.math.abs(totalX) < tapSlopPx && kotlin.math.abs(totalY) < tapSlopPx) {
                                    // Tap: pilih kata di bawah jari (teks dimulai di (0,0) Box).
                                    val layout = textLayout
                                    if (layout != null) {
                                        val charOffset = layout.getOffsetForPosition(lastPos)
                                        val idx = wordIndexAt(charOffset, wordOffsets)
                                        if (idx in words.indices) {
                                            onSelectWord(if (idx == state.selectedWordIndex) -1 else idx)
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        BasicText(
                            text = annotated,
                            onTextLayout = { textLayout = it },
                            style = AyahTypography.ArabicWord.copy(
                                color = AyahColors.TextPrimary,
                                fontSize = (20 * state.fontScale).sp,
                                lineHeight = (20 * state.fontScale * 2.4f).sp,
                                textAlign = TextAlign.Start,
                                fontFamily = state.arabicFontFamily,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // Tooltip mengambang di bawah kata yang dipilih.
                    val sel = state.selectedWordIndex
                    val layout = textLayout
                    if (sel != null && layout != null && sel < words.size) {
                        val rect = layout.getBoundingBox(wordOffsets[sel])
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(rect.left.roundToInt(), rect.bottom.roundToInt() + 6),
                            onDismissRequest = { onSelectWord(-1) },
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
                                            words[sel],
                                            style = AyahTypography.Arabic.copy(
                                                fontSize = 20.sp,
                                                fontFamily = state.arabicFontFamily,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AyahButton(
                                            text = if (state.isWordPlaying) strings.stop else strings.playWord,
                                            variant = if (state.isWordPlaying) {
                                                AyahButtonVariant.Danger
                                            } else {
                                                AyahButtonVariant.Secondary
                                            },
                                            size = AyahButtonSize.Small,
                                            onClick = if (state.isWordPlaying) onStopSelectedWord else onPlaySelectedWord,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val rules = state.selectedWordRules
                                    if (rules.isEmpty()) {
                                        AyahText(
                                            strings.noTajwidRule,
                                            style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
                                        )
                                    } else {
                                        rules.forEach { r ->
                                            val exp = if (state.language == AppLanguage.EN) r.explanationEn else r.explanation
                                            AyahText(
                                                "• ${r.name} — $exp",
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
                // Terjemahan ayat (bahasa aktif).
                val translation = ayah?.translation.orEmpty()
                if (translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AyahText(
                        translation,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Ruang kosong di bawah terjemahan — tetap bisa di-swipe.
                Spacer(modifier = Modifier.height(24.dp))

        // ---- Transkrip real-time ----
        if (state.listening) {
            Spacer(modifier = Modifier.height(8.dp))
            AyahText(
                "${strings.detectedPrefix} ${state.transcript.ifBlank { "…" }}",
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Daftar kesalahan bacaan ----
        if (state.issues.isNotEmpty()) {
            AyahText(
                "${strings.issuesTitle} (${state.issues.size})",
                style = AyahTypography.Heading2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.issues.forEach { issue ->
                IssueCard(
                    issue = issue,
                    strings = strings,
                    fontScale = state.fontScale,
                    fontFamily = state.arabicFontFamily,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- Info riwayat ayat aktif (dari statistik persisten) ----
        state.ayahStats?.let { st ->
            if (st.attempts > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                AyahText(
                    strings.statsInline.format(st.attempts, st.bestScore),
                    style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                )
            }
        }

        // ---- Pesan sistem ----
        state.message?.let { msg ->
            AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onDismissMessage) {
                AyahText(msg, style = AyahTypography.Body2, color = AyahColors.Error)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

            // Penutup area swipe: mushaf → terjemahan → kosong → kesalahan.
            }
        }

            Spacer(modifier = Modifier.height(16.dp))
            }

            // ---- Progress unduh audio (di ATAS tombol mic & dengar) ----
            DownloadFooter(
                isDownloading = state.isDownloading,
                done = state.downloadDone,
                total = state.downloadTotal,
                strings = strings,
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
                        if (state.listening) strings.listeningHint
                        else strings.micHint,
                        style = AyahTypography.Caption,
                        modifier = Modifier.weight(1f),
                    )
                    AyahButton(
                        text = if (state.isAudioPlaying) strings.stop else strings.listen,
                        variant = if (state.isAudioPlaying) AyahButtonVariant.Danger else AyahButtonVariant.Secondary,
                        onClick = onToggleAudioPlayback,
                    )
                }
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

@Composable
private fun IssueCard(
    issue: ReadingIssue,
    strings: Strings,
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
                "${strings.issueDetected} $spoken",
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

// ============================================================ Footer unduh

/** Footer progress unduh audio (di paling bawah, tidak nge-block view). */
@Composable
private fun DownloadFooter(
    isDownloading: Boolean,
    done: Int,
    total: Int,
    strings: Strings,
) {
    if (!isDownloading) return
    // Surah yang sedang diunduh (dari status global unduhan).
    val dl by DownloadProgress.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AyahColors.Background)
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AyahText(
                if (dl.currentSurahName != null) {
                    "${strings.downloadingLabel} ${dl.currentSurahName}"
                } else {
                    strings.downloadingLabel
                },
                style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/**
 * Indeks kata yang mengandung offset karakter pada teks mushaf kontinu.
 * (spasi jatuh ke kata sebelumnya — ketukan di dekat kata tetap memilih kata itu)
 */
private fun wordIndexAt(offset: Int, wordOffsets: IntArray): Int {
    var result = -1
    for (i in wordOffsets.indices) {
        if (offset >= wordOffsets[i]) result = i else break
    }
    return result
}

/**
 * Bangun teks mushaf satu ayat (AnnotatedString): kata tersambung dengan
 * spasi, warna huruf tajwid per huruf, latar status bacaan per kata,
 * dan highlight kata yang dipilih.
 */
private fun buildAyahAnnotated(
    words: List<String>,
    spansByWord: List<List<TajwidSpan>>,
    statusByIndex: Map<Int, AlignedWord>,
    selectedIndex: Int?,
    tajwidColor: Boolean,
): AnnotatedString = buildAnnotatedString {
    words.forEachIndexed { index, word ->
        val start = length
        append(word)
        val end = length
        if (index < words.size - 1) append(' ')
        if (tajwidColor) {
            for (sp in spansByWord.getOrNull(index).orEmpty()) {
                val color = tajwidColorFor(sp.category) ?: continue
                addStyle(SpanStyle(color = color), start + sp.start, start + sp.end.coerceAtMost(word.length))
            }
        }
        val bg = when (statusByIndex[index]?.status) {
            WordStatus.CORRECT -> AyahColors.Success.copy(alpha = 0.22f)
            WordStatus.MISMATCH -> AyahColors.Error.copy(alpha = 0.22f)
            WordStatus.READING -> AyahColors.Reading.copy(alpha = 0.5f)
            WordStatus.SKIPPED -> AyahColors.Error.copy(alpha = 0.12f)
            else -> null
        }
        if (bg != null) addStyle(SpanStyle(background = bg), start, end)
        if (selectedIndex == index) {
            addStyle(SpanStyle(background = AyahColors.PrimarySoft), start, end)
        }
    }
}
