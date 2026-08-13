package org.opennur.tahsin.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import org.opennur.tahsin.data.quran.AyahNumbering
import org.opennur.tahsin.data.quran.Basmalah
import org.opennur.tahsin.data.quran.ComposedPage
import org.opennur.tahsin.data.quran.MushafAyah
import org.opennur.tahsin.data.quran.MushafPageComposer
import org.opennur.tahsin.data.quran.SajdahSigns
import org.opennur.tahsin.data.tajwid.RuleCategory
import org.opennur.tahsin.data.tajwid.TajwidColorizer
import org.opennur.tahsin.data.tajwid.TajwidEngine
import org.opennur.tahsin.data.tajwid.TajwidSpan
import org.opennur.tahsin.stt.AlignedWord
import org.opennur.tahsin.stt.WordStatus
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTypography
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.DownloadProgress
import org.opennur.tahsin.util.FontScales
import org.opennur.tahsin.ui.components.AyahButton
import org.opennur.tahsin.ui.components.AyahButtonSize
import org.opennur.tahsin.ui.components.AyahButtonVariant
import org.opennur.tahsin.ui.components.AyahCard
import org.opennur.tahsin.ui.components.AyahErrorView
import org.opennur.tahsin.ui.components.AyahLoadingView
import org.opennur.tahsin.ui.components.AyahText
import org.opennur.tahsin.ui.components.DropdownOption
import org.opennur.tahsin.ui.components.SimpleDropdown
import kotlin.math.roundToInt

/**
 * Layar Tahsin — mushaf halaman (604 halaman Madani): baca seperti membuka
 * mushaf asli (flip halaman RTL), tanda akhir ayat ۝+nomor, tanda sujud ۩,
 * basmalah di awal surah, terjemahan disembunyikan secara default. Ketuk
 * ayat → jadikan ayat aktif, latihan STT inline (warna kata + umpan balik).
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
            onSelectPage = viewModel::selectPage,
            onJumpToSurah = viewModel::jumpToSurah,
            onJumpToPage = viewModel::jumpToPage,
            onSelectAyah = viewModel::selectAyahAt,
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
            onToggleTranslation = viewModel::toggleTranslation,
            onSetFontScale = viewModel::setFontScale,
            onSetAudioMode = viewModel::setAudioMode,
            onDismissMessage = viewModel::clearMessage,
            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
            onOpenSearch = onOpenSearch,
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

// ============================================================ Konten utama

@Composable
private fun TahsinContent(
    state: TahsinUiState.Ready,
    onSelectPage: (Int) -> Unit,
    onJumpToSurah: (Int) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onSelectAyah: (Int, Int) -> Unit,
    onSelectWord: (Int) -> Unit,
    onMicClick: () -> Unit,
    onPlaySelectedWord: () -> Unit,
    onStopSelectedWord: () -> Unit,
    onToggleTranslation: () -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetAudioMode: (AudioPlaybackMode) -> Unit,
    onDismissMessage: () -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = AppStrings.of(state.language)

    // Pager halaman mushaf — alur RTL (halaman 1 di kanan, seperti mushaf asli).
    val pagerState = rememberPagerState(initialPage = state.pageIndex) { state.pageCount }
    // VM pindah halaman (jump surah/juz, openAt, auto-advance) → pager mengikuti.
    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) pagerState.scrollToPage(state.pageIndex)
    }
    // User menggeser pager → VM memuat & menyusun halaman baru.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.pageIndex) onSelectPage(pagerState.currentPage)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AyahColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // ---- Header: kembali, judul, toggle terjemahan, pencarian, pengaturan ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyahButton(text = "←", variant = AyahButtonVariant.Outline, size = AyahButtonSize.Small, onClick = onBack)
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(strings.appTitle, style = AyahTypography.Heading1, modifier = Modifier.weight(1f))
            AyahButton(
                text = strings.tahsinTranslation,
                variant = if (state.showTranslation) AyahButtonVariant.Primary else AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                onClick = onToggleTranslation,
            )
            Spacer(modifier = Modifier.width(6.dp))
            AyahButton(text = "🔍", variant = AyahButtonVariant.Outline, size = AyahButtonSize.Small, onClick = onOpenSearch)
            Spacer(modifier = Modifier.width(6.dp))
            AyahButton(text = "⚙", variant = AyahButtonVariant.Outline, size = AyahButtonSize.Small, onClick = onOpenSettings)
        }

        // ---- Navigasi lompat: surah / juz + indikator halaman ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleDropdown(
                selectedLabel = state.surah?.let { "${it.number}. ${it.nameLatin}" } ?: "-",
                options = state.surahs.map { s ->
                    DropdownOption("${s.number}. ${s.nameLatin}", { onJumpToSurah(s.number) })
                },
                modifier = Modifier.weight(2f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SimpleDropdown(
                // Label cukup nomor halaman (Arab-Indik) — "Halaman" terlalu panjang
                // dan kepotong di dropdown yang sempit.
                selectedLabel = AyahNumbering.toArabicIndic(state.pageIndex + 1),
                options = (1..state.pageCount).map { p ->
                    DropdownOption("${strings.tahsinPage} ${AyahNumbering.toArabicIndic(p)}", { onJumpToPage(p) })
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                "${AyahNumbering.toArabicIndic(state.pageIndex + 1)} / ${AyahNumbering.toArabicIndic(state.pageCount)}",
                style = AyahTypography.Caption.copy(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
            )
        }

        // ---- Kontrol ukuran huruf (A− / A+) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyahButton(
                text = "A−",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                enabled = state.fontScale > FontScales.MIN,
                onClick = { onSetFontScale(state.fontScale - FontScales.STEP) },
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(
                "${(state.fontScale * 100).roundToInt()}%",
                style = AyahTypography.Caption.copy(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahButton(
                text = "A+",
                variant = AyahButtonVariant.Outline,
                size = AyahButtonSize.Small,
                enabled = state.fontScale < FontScales.MAX,
                onClick = { onSetFontScale(state.fontScale + FontScales.STEP) },
            )
        }

        // ---- Pager halaman mushaf ----
        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            MushafPageView(
                state = state,
                pageIndex = pageIndex,
                strings = strings,
                onSelectAyah = onSelectAyah,
                onSelectWord = onSelectWord,
                onPlaySelectedWord = onPlaySelectedWord,
                onStopSelectedWord = onStopSelectedWord,
                onDismissMessage = onDismissMessage,
            )
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
                    if (state.listening) strings.listeningHint else strings.micHint,
                    style = AyahTypography.Caption,
                    modifier = Modifier.weight(1f),
                )
                SimpleDropdown(
                    selectedLabel = audioModeSymbol(state.audioMode),
                    options = listOf(
                        DropdownOption("١ — ${strings.tahsinAudioSingle}", { onSetAudioMode(AudioPlaybackMode.AYAH) }),
                        DropdownOption("→ — ${strings.tahsinAudioContinuous}", { onSetAudioMode(AudioPlaybackMode.CONTINUOUS) }),
                        DropdownOption("↻ — ${strings.tahsinAudioRepeat}", { onSetAudioMode(AudioPlaybackMode.REPEAT) }),
                    ),
                    modifier = Modifier.width(88.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AyahButton(
                    text = if (state.isAudioPlaying) strings.stop else strings.listen,
                    variant = if (state.isAudioPlaying) AyahButtonVariant.Danger else AyahButtonVariant.Secondary,
                    onClick = onToggleAudioPlayback,
                )
            }
        }
    }
}

/** Susun halaman [pageIndex] dari konten surah yang sudah dimuat (null = belum siap). */
private fun composePage(state: TahsinUiState.Ready, pageIndex: Int): ComposedPage? =
    MushafPageComposer.composePage(
        state.pagination,
        pageIndex + 1,
        state.surahs.associateBy { it.number },
    )

// ============================================================ Satu halaman

@Composable
private fun MushafPageView(
    state: TahsinUiState.Ready,
    pageIndex: Int,
    strings: Strings,
    onSelectAyah: (Int, Int) -> Unit,
    onSelectWord: (Int) -> Unit,
    onPlaySelectedWord: () -> Unit,
    onStopSelectedWord: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val composed = remember(state.surahs, state.pagination, pageIndex) {
        composePage(state, pageIndex)
    }
    val isCurrentPage = pageIndex == state.pageIndex
    val ayah = state.ayah
    val activeOnPage = composed?.ayahs?.any {
        it.surah == state.surahNumber && it.number == state.ayahIndex + 1
    } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // ---- Band header ala mushaf Madani ----
        if (composed != null) {
            PageHeaderBand(composed = composed, strings = strings, fontFamily = state.arabicFontFamily)
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (composed == null) {
            AyahCard(modifier = Modifier.fillMaxWidth()) {
                AyahText(
                    strings.loadingSurah,
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            // ---- Isi halaman: ayat-ayat mushaf ----
            var prevSurah: Int? = null
            composed.ayahs.forEach { entry ->
                if (prevSurah != null && entry.surah != prevSurah) {
                    // Surah baru mulai di tengah halaman (umum di juz 30):
                    // pembatas + nama surah, ala mushaf cetak.
                    Spacer(modifier = Modifier.height(8.dp))
                    SurahDivider(
                        nameArabic = state.surahs.firstOrNull { it.number == entry.surah }?.nameArabic.orEmpty(),
                        fontFamily = state.arabicFontFamily,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                prevSurah = entry.surah
                if (entry.hasBasmalah) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BasmalahRow(fontFamily = state.arabicFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                val isActive = entry.surah == state.surahNumber && entry.number == state.ayahIndex + 1
                AyahBlock(
                    entry = entry,
                    isActive = isActive,
                    state = state,
                    strings = strings,
                    onSelectAyah = onSelectAyah,
                    onSelectWord = onSelectWord,
                    onPlaySelectedWord = onPlaySelectedWord,
                    onStopSelectedWord = onStopSelectedWord,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // ---- Ekstra latihan HANYA di halaman aktif (praktik STT) ----
            if (isCurrentPage && ayah != null && activeOnPage) {
                // Terjemahan ayat aktif — tersembunyi kecuali toggle dinyalakan.
                if (state.showTranslation && ayah.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Indikator: terjemahan ini untuk ayat ke berapa.
                    AyahText(
                        "${state.surah?.nameLatin ?: ""} ${AyahNumbering.toArabicIndic(state.ayahIndex + 1)}",
                        style = AyahTypography.Caption.copy(
                            color = AyahColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AyahText(
                        ayah.translation,
                        style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                // Transkrip real-time.
                if (state.listening) {
                    AyahText(
                        "${strings.detectedPrefix} ${state.transcript.ifBlank { "…" }}",
                        style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Daftar kesalahan bacaan.
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
                            language = state.language,
                            fontScale = state.fontScale,
                            fontFamily = state.arabicFontFamily,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Info riwayat ayat aktif.
                state.ayahStats?.let { st ->
                    if (st.attempts > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AyahText(
                            strings.statsInline.format(st.attempts, st.bestScore),
                            style = AyahTypography.Caption.copy(color = AyahColors.Primary),
                        )
                    }
                }
            }

            // ---- Pesan sistem ----
            if (isCurrentPage) {
                state.message?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AyahCard(modifier = Modifier.fillMaxWidth(), onClick = onDismissMessage) {
                        AyahText(msg, style = AyahTypography.Body2, color = AyahColors.Error)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Band header halaman ala mushaf Madani: nomor halaman, nama surah, juz. */
@Composable
private fun PageHeaderBand(
    composed: ComposedPage,
    strings: Strings,
    fontFamily: FontFamily,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AyahColors.SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahText(
                    AyahNumbering.toArabicIndic(composed.page),
                    style = AyahTypography.Arabic.copy(
                        fontSize = 13.sp,
                        fontFamily = fontFamily,
                        color = AyahColors.TextSecondary,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                AyahText(
                    composed.surahNameArabic,
                    style = AyahTypography.Arabic.copy(
                        fontSize = 18.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = AyahColors.TextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                AyahText(
                    "${strings.tahsinJuz} ${AyahNumbering.toArabicIndic(composed.juz)}",
                    style = AyahTypography.Caption.copy(
                        color = if (composed.juzStartsOnPage) AyahColors.Primary else AyahColors.TextSecondary,
                        fontWeight = if (composed.juzStartsOnPage) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
            if (composed.juzStartsOnPage) {
                Spacer(modifier = Modifier.height(2.dp))
                AyahText(
                    "❁ ${strings.tahsinJuz} ${AyahNumbering.toArabicIndic(composed.juz)}",
                    style = AyahTypography.Caption.copy(
                        color = AyahColors.Primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Pembatas antar-surah dalam satu halaman: garis + nama surah (ala mushaf cetak). */
@Composable
private fun SurahDivider(nameArabic: String, fontFamily: FontFamily) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AyahColors.Divider))
        AyahText(
            nameArabic,
            style = AyahTypography.Arabic.copy(
                fontSize = 15.sp,
                fontFamily = fontFamily,
                color = AyahColors.Primary,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(AyahColors.Divider))
    }
}

/** Ornamen basmalah di awal surah (kecuali At-Tawbah; Al-Fatihah tidak ganda). */
@Composable
private fun BasmalahRow(fontFamily: FontFamily) {
    AyahText(
        Basmalah.TEXT,
        style = AyahTypography.Arabic.copy(
            fontSize = 20.sp,
            fontFamily = fontFamily,
            color = AyahColors.Primary,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Satu ayat di halaman: teks kata-berwarna-tajwid (konsisten aktif/non-aktif)
 *  + badge akhir ayat (lingkaran nomor Arab-Indik) + badge sujud. Mengaktifkan
 *  ayat TIDAK mengubah ukuran/posisi teks (hanya tint latar + interaksi). */
@Composable
private fun AyahBlock(
    entry: MushafAyah,
    isActive: Boolean,
    state: TahsinUiState.Ready,
    strings: Strings,
    onSelectAyah: (Int, Int) -> Unit,
    onSelectWord: (Int) -> Unit,
    onPlaySelectedWord: () -> Unit,
    onStopSelectedWord: () -> Unit,
) {
    val words = remember(entry.text) {
        org.opennur.tahsin.data.quran.Ayah(entry.number, entry.text).words
    }
    // Status bacaan STT hanya untuk ayat aktif; warna TAJWID untuk SEMUA ayat.
    val statusByIndex = if (isActive) state.alignedWords.associateBy { it.index } else emptyMap()
    val selectedIndex = if (isActive) state.selectedWordIndex else null
    val spansByWord = remember(words) {
        words.mapIndexed { idx, w ->
            TajwidColorizer.spans(
                w,
                TajwidEngine.analyzeWord(w, words.getOrNull(idx - 1), words.getOrNull(idx + 1)),
            )
        }
    }
    val annotated = remember(words, spansByWord, statusByIndex, selectedIndex, state.tajwidColor) {
        buildAyahAnnotated(
            words = words,
            spansByWord = spansByWord,
            statusByIndex = statusByIndex,
            selectedIndex = selectedIndex,
            tajwidColor = state.tajwidColor,
        )
    }
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

    // Baris RTL: teks ayat di kanan, badge akhir ayat & sujud di ujung kiri
    // (posisi akhir baris terakhir — ala mushaf). Tint latar TANPA padding untuk
    // ayat aktif supaya ukuran/posisi teks tidak berubah saat diketuk.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isActive) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AyahColors.PrimarySoft.copy(alpha = 0.45f))
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Kotak teks: ketukan & tooltip berlabuh di sini (koordinat = teks).
            Box(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = annotated,
                    onTextLayout = { textLayout = it },
                    style = AyahTypography.ArabicWord.copy(
                        color = AyahColors.TextPrimary,
                        fontSize = (14 * state.fontScale).sp,
                        lineHeight = (14 * state.fontScale * 2.2f).sp,
                        textAlign = TextAlign.Start,
                        fontFamily = state.arabicFontFamily,
                    ),
                    // Satu jalur ketukan untuk aktif & non-aktif (detectTapGestures
                    // TIDAK memaksa ukuran sentuh minimum 48dp seperti clickable —
                    // mengetuk ayat pendek tidak mengubah tinggi baris).
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(state.selectedWordIndex, isActive) {
                            detectTapGestures { position ->
                                if (isActive) {
                                    val layout = textLayout
                                    if (layout != null) {
                                        val charOffset = layout.getOffsetForPosition(position)
                                        val idx = wordIndexAt(charOffset, wordOffsets)
                                        if (idx in words.indices) {
                                            onSelectWord(if (idx == selectedIndex) -1 else idx)
                                        }
                                    }
                                } else {
                                    onSelectAyah(entry.surah, entry.number)
                                }
                            }
                        },
                )
                // Tooltip kata terpilih (hanya ayat aktif) — di dalam kotak teks
                // supaya koordinat rect (relatif teks) sama dengan jangkar Popup.
                if (isActive) {
                    val sel = selectedIndex
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
            Spacer(modifier = Modifier.width(6.dp))
            AyahEndBadge(entry.number)
            if (entry.isSajdah) {
                Spacer(modifier = Modifier.width(6.dp))
                SajdahBadge()
            }
        }
    }
}

/** Badge akhir ayat: lingkaran kecil + nomor Arab-Indik (pengganti glif ۝). */
@Composable
private fun AyahEndBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(AyahColors.PrimarySoft, CircleShape)
            .border(1.dp, AyahColors.Primary.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AyahText(
            AyahNumbering.toArabicIndic(number),
            style = AyahTypography.Caption.copy(fontSize = 11.sp, color = AyahColors.Primary),
        )
    }
}

/** Badge sujud tilawah: lingkaran kecil dengan tanda ۩. */
@Composable
private fun SajdahBadge() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(AyahColors.Primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AyahText(
            SajdahSigns.SIGN,
            style = AyahTypography.Caption.copy(fontSize = 14.sp, color = Color.White),
        )
    }
}

// ============================================================ Sub-komponen

@Composable
private fun IssueCard(
    issue: ReadingIssue,
    strings: Strings,
    language: AppLanguage,
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
                val exp = if (language == AppLanguage.EN) rule.explanationEn else rule.explanation
                AyahText(
                    "• ${rule.name} — $exp",
                    style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            AyahText(
                strings.issueFallback,
                style = AyahTypography.Body2.copy(color = AyahColors.TextSecondary),
            )
        }
    }
}

/** Simbol mode pemutaran audio untuk tombol di samping "Dengar". */
private fun audioModeSymbol(mode: AudioPlaybackMode): String = when (mode) {
    AudioPlaybackMode.AYAH -> "١"
    AudioPlaybackMode.CONTINUOUS -> "→"
    AudioPlaybackMode.REPEAT -> "↻"
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
            addStyle(
                SpanStyle(
                    background = AyahColors.Primary.copy(alpha = 0.42f),
                    color = AyahColors.Primary,
                ),
                start,
                end,
            )
        }
    }
}
