package org.opennur.tahsin.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
            // 3 dropdown navigasi: [Surah] [Ayat] [Halaman]. Label dipilih yang
            // PENDEK (nama surah tanpa nomor, nomor Arab-Indik dalam kurung)
            // supaya tidak terpotong "..." di layar sempit.
            SimpleDropdown(
                selectedLabel = state.surah?.nameLatin ?: "-",
                options = state.surahs.map { s ->
                    DropdownOption("${s.number}. ${s.nameLatin}", { onJumpToSurah(s.number) })
                },
                modifier = Modifier.weight(2f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SimpleDropdown(
                selectedLabel = "[${AyahNumbering.toArabicIndic(state.ayahIndex + 1)}]",
                options = (1..(state.surah?.ayahCount ?: 0)).map { a ->
                    DropdownOption(
                        "${strings.tahsinAyah} ${AyahNumbering.toArabicIndic(a)}",
                        { onSelectAyah(state.surahNumber, a) },
                    )
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            SimpleDropdown(
                selectedLabel = "[${AyahNumbering.toArabicIndic(state.pageIndex + 1)}]",
                options = (1..state.pageCount).map { p ->
                    DropdownOption("${strings.tahsinPage} ${AyahNumbering.toArabicIndic(p)}", { onJumpToPage(p) })
                },
                modifier = Modifier.weight(1f),
            )
        }

        // ---- Kontrol ukuran huruf (slider presisi) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyahText(
                "A−",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.width(8.dp))
            FontSizeSlider(
                value = state.fontScale,
                onValueChange = onSetFontScale,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AyahText(
                "A+",
                style = AyahTypography.Caption.copy(color = AyahColors.TextSecondary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            AyahText(
                "${(state.fontScale * 100).roundToInt()}%",
                style = AyahTypography.Caption.copy(
                    color = AyahColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.width(44.dp),
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
                    modifier = Modifier.width(52.dp),
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
            // ---- Isi halaman: teks ayat MENGALIR per surah (RTL menyambung
            //      seperti mushaf cetak) — bukan satu ayat per baris ----
            val groups = remember(composed) { composed.ayahs.groupBy { it.surah } }
            groups.forEach { (surahNumber, groupAyahs) ->
                if (surahNumber != composed.ayahs.first().surah) {
                    // Surah baru mulai di tengah halaman (umum di juz 30):
                    // pembatas + nama surah, ala mushaf cetak.
                    Spacer(modifier = Modifier.height(10.dp))
                    SurahDivider(
                        nameArabic = state.surahs.firstOrNull { it.number == surahNumber }?.nameArabic.orEmpty(),
                        fontFamily = state.arabicFontFamily,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (groupAyahs.first().hasBasmalah) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BasmalahRow(fontFamily = state.arabicFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                SurahFlowBlock(
                    ayahs = groupAyahs,
                    state = state,
                    strings = strings,
                    onSelectAyah = onSelectAyah,
                    onSelectWord = onSelectWord,
                    onPlaySelectedWord = onPlaySelectedWord,
                    onStopSelectedWord = onStopSelectedWord,
                )
                Spacer(modifier = Modifier.height(8.dp))
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
/** Blok teks MENGALIR satu surah: ayat-ayat disambung dari kanan ke kiri
 *  seperti mushaf (ayat pendek tidak lagi satu-satu di baris baru). Badge
 *  akhir ayat & sujud digambar di posisi akhir tiap ayat; highlight ayat aktif
 *  digambar DI BAWAH teks per baris, jadi harakat/tanda waqaf yang overflow
 *  tidak terpotong. */
@Composable
private fun SurahFlowBlock(
    ayahs: List<MushafAyah>,
    state: TahsinUiState.Ready,
    strings: Strings,
    onSelectAyah: (Int, Int) -> Unit,
    onSelectWord: (Int) -> Unit,
    onPlaySelectedWord: () -> Unit,
    onStopSelectedWord: () -> Unit,
) {
    val flow = remember(ayahs) { buildFlowMeta(ayahs) }
    val activeIdx = ayahs.indexOfFirst {
        it.surah == state.surahNumber && it.number == state.ayahIndex + 1
    }
    val statusByIndex = if (activeIdx >= 0) state.alignedWords.associateBy { it.index } else emptyMap()
    val selectedIndex = if (activeIdx >= 0) state.selectedWordIndex else null
    val spansByWord = remember(flow) {
        flow.wordsByAyah.mapIndexed { i, tokens ->
            tokens.mapIndexed { j, w ->
                TajwidColorizer.spans(
                    w,
                    TajwidEngine.analyzeWord(w, tokens.getOrNull(j - 1), tokens.getOrNull(j + 1)),
                )
            }
        }
    }
    val annotated = remember(flow, activeIdx, statusByIndex, selectedIndex, state.tajwidColor) {
        buildFlowAnnotated(
            fullText = flow.fullText,
            wordStarts = flow.wordStarts,
            wordsByAyah = flow.wordsByAyah,
            spansByWord = spansByWord,
            activeIdx = activeIdx,
            statusByIndex = statusByIndex,
            selectedIndex = selectedIndex,
            tajwidColor = state.tajwidColor,
        )
    }
    var textLayout by remember(flow) { mutableStateOf<TextLayoutResult?>(null) }
    val textMeasurer = rememberTextMeasurer()
    // Ruang cadang kiri (RTL) agar badge di ujung baris penuh tidak terpotong:
    // 28dp untuk nomor ayat + 30dp ekstra kalau blok punya ayat sajdah.
    val blockReserve = 28.dp + if (ayahs.any { it.isSajdah }) 30.dp else 0.dp

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = blockReserve)
                    // Highlight ayat aktif DI BAWAH teks (tanpa clip) — harakat/
                    // tanda waqaf yang overflow tidak terpotong.
                    .drawBehind {
                        val layout = textLayout
                        val ai = activeIdx
                        if (layout != null && ai >= 0) {
                            drawActiveAyahHighlight(layout, flow.ayahStart[ai], flow.ayahEnd[ai])
                        }
                    }
                    // Badge akhir ayat & sujud digambar di ujung tiap ayat.
                    .drawWithContent {
                        drawContent()
                        val layout = textLayout
                        if (layout != null) {
                            drawAyahBadges(
                                layout = layout,
                                flow = flow,
                                ayahs = ayahs,
                                measurer = textMeasurer,
                            )
                        }
                    }
                    .pointerInput(activeIdx, selectedIndex, ayahs) {
                        detectTapGestures { position ->
                            val layout = textLayout
                            if (layout != null) {
                                val offset = layout.getOffsetForPosition(position)
                                val ai2 = ayahIndexAt(offset, flow.ayahStart, flow.ayahEnd)
                                if (ai2 in ayahs.indices) {
                                    val entry = ayahs[ai2]
                                    if (ai2 == activeIdx) {
                                        val ws = flow.wordStarts[ai2]
                                        val words = flow.wordsByAyah[ai2]
                                        // Offset di separator/badge (lewat kata
                                        // terakhir) = bukan kata — jangan buka tooltip.
                                        val lastEnd = ws.lastOrNull()?.let {
                                            it + (words.lastOrNull()?.length ?: 0)
                                        } ?: 0
                                        val idx = if (offset < lastEnd) wordIndexAt(offset, ws) else -1
                                        if (idx in ws.indices) {
                                            onSelectWord(if (idx == selectedIndex) -1 else idx)
                                        }
                                    } else {
                                        onSelectAyah(entry.surah, entry.number)
                                    }
                                }
                            }
                        }
                    },
            )
            // Tooltip kata terpilih (hanya ayat aktif) — koordinat sama dengan teks.
            if (activeIdx >= 0) {
                val sel = selectedIndex
                val layout = textLayout
                if (sel != null && layout != null) {
                    val ws = flow.wordStarts[activeIdx]
                    if (sel < ws.size) {
                        val rect = layout.getBoundingBox(ws[sel])
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(rect.left.roundToInt(), rect.bottom.roundToInt() + 6),
                            onDismissRequest = { onSelectWord(-1) },
                        ) {
                            // Tooltip pakai arah LTR supaya keterangan tajwid
                            // rata KIRI (jangan ikut RTL mushaf).
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                            flow.wordsByAyah[activeIdx].getOrNull(sel) ?: "",
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
            }
        }
    }
}

/** Badge akhir ayat & sujud digambar di posisi akhir tiap ayat (blok mengalir). */
private fun DrawScope.drawAyahBadges(
    layout: TextLayoutResult,
    flow: FlowMeta,
    ayahs: List<MushafAyah>,
    measurer: TextMeasurer,
) {
    val radius = 12.dp.toPx()
    for (i in ayahs.indices) {
        // Ujung teks ayat i = kursor setelah karakter terakhirnya (RTL).
        val end = layout.getCursorRect(flow.ayahEnd[i])
        val centerY = end.top + end.height / 2f
        val centerX = end.left - radius - 2.dp.toPx()
        drawVerseNumberBadge(
            center = Offset(centerX, centerY),
            number = ayahs[i].number,
            measurer = measurer,
            radius = radius,
        )
        if (flow.sajdah[i]) {
            drawSajdahBadge(
                center = Offset(centerX - 26.dp.toPx(), centerY),
                measurer = measurer,
                radius = radius - 2.dp.toPx(),
            )
        }
    }
}

/** Highlight ayat aktif: rounded rect per baris DI BAWAH teks (tanpa clip). */
private fun DrawScope.drawActiveAyahHighlight(
    layout: TextLayoutResult,
    ayahStart: Int,
    ayahEnd: Int,
) {
    val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    for (line in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true)
        val a = maxOf(ayahStart, lineStart)
        val b = minOf(ayahEnd, lineEnd)
        if (a >= b) continue
        // Ekstensi horizontal segmen = gabungan kotak karakter pertama & terakhir.
        val first = layout.getBoundingBox(a)
        val last = layout.getBoundingBox(b - 1)
        val left = minOf(first.left, last.left)
        val right = maxOf(first.right, last.right)
        drawRoundRect(
            color = AyahColors.PrimarySoft.copy(alpha = 0.45f),
            topLeft = Offset(left, layout.getLineTop(line)),
            size = Size(right - left, layout.getLineBottom(line) - layout.getLineTop(line)),
            cornerRadius = corner,
        )
    }
}

private fun DrawScope.drawVerseNumberBadge(
    center: Offset,
    number: Int,
    measurer: TextMeasurer,
    radius: Float,
) {
    drawCircle(color = AyahColors.PrimarySoft, radius = radius, center = center)
    drawCircle(
        color = AyahColors.Primary.copy(alpha = 0.55f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    val label = measurer.measure(
        text = AnnotatedString(AyahNumbering.toArabicIndic(number)),
        style = AyahTypography.Caption.copy(fontSize = 11.sp, color = AyahColors.Primary),
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(center.x - label.size.width / 2f, center.y - label.size.height / 2f),
    )
}

/** Badge sujud tilawah (lingkaran penuh + ۩) di sebelah kiri badge nomor. */
private fun DrawScope.drawSajdahBadge(center: Offset, measurer: TextMeasurer, radius: Float) {
    drawCircle(color = AyahColors.Primary, radius = radius, center = center)
    val label = measurer.measure(
        text = AnnotatedString(SajdahSigns.SIGN),
        style = AyahTypography.Caption.copy(fontSize = 14.sp, color = Color.White),
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(center.x - label.size.width / 2f, center.y - label.size.height / 2f),
    )
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

/** Slider ukuran huruf — design system sendiri (aplikasi tanpa material3). */
@Composable
private fun FontSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val min = FontScales.MIN
    val max = FontScales.MAX
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val thumbPx = with(LocalDensity.current) { 20.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p ->
                        onValueChange(fontScaleAtX(p.x, size.width.toFloat(), thumbPx, min, max))
                    },
                    onDrag = { change, _ ->
                        onValueChange(fontScaleAtX(change.position.x, size.width.toFloat(), thumbPx, min, max))
                        change.consume()
                    },
                )
            },
    ) {
        val trackWidth = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbCenter = thumbPx / 2f + fraction * (trackWidth - thumbPx)
        // Rel (latar track).
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AyahColors.SurfaceVariant),
        )
        // Bagian terisi.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AyahColors.Primary),
        )
        // Thumb.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((thumbCenter - thumbPx / 2f).roundToInt(), 0) }
                .size(20.dp)
                .background(AyahColors.Primary, CircleShape)
                .border(2.dp, AyahColors.Background, CircleShape),
        )
    }
}

/** Peta posisi x (px) di track ke fontScale (MIN..MAX); thumb menempel ujung. */
private fun fontScaleAtX(x: Float, width: Float, thumbSize: Float, min: Float, max: Float): Float {
    if (width <= thumbSize) return min
    val usable = width - thumbSize
    val f = ((x - thumbSize / 2f) / usable).coerceIn(0f, 1f)
    return min + f * (max - min)
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
 * Bangun AnnotatedString blok mengalir: teks penuh (tanda waqaf ikut) + warna
 * tajwid per huruf + latar status STT + latar kata terpilih (ayat aktif saja).
 */
private fun buildFlowAnnotated(
    fullText: String,
    wordStarts: List<IntArray>,
    wordsByAyah: List<List<String>>,
    spansByWord: List<List<List<TajwidSpan>>>,
    activeIdx: Int,
    statusByIndex: Map<Int, AlignedWord>,
    selectedIndex: Int?,
    tajwidColor: Boolean,
): AnnotatedString = buildAnnotatedString {
    append(fullText)
    wordStarts.forEachIndexed { i, ws ->
        val words = wordsByAyah[i]
        val isActive = i == activeIdx
        ws.forEachIndexed { j, start ->
            val wordLength = words.getOrNull(j)?.length ?: 0
            val s0 = start.coerceIn(0, fullText.length)
            val end = (s0 + wordLength).coerceAtMost(fullText.length)
            if (tajwidColor) {
                for (sp in spansByWord[i].getOrNull(j).orEmpty()) {
                    val color = tajwidColorFor(sp.category) ?: continue
                    addStyle(
                        SpanStyle(color = color),
                        s0 + sp.start.coerceAtMost(wordLength),
                        (s0 + sp.end.coerceAtMost(wordLength)).coerceAtMost(fullText.length),
                    )
                }
            }
            if (isActive) {
                val bg = when (statusByIndex[j]?.status) {
                    WordStatus.CORRECT -> AyahColors.Success.copy(alpha = 0.22f)
                    WordStatus.MISMATCH -> AyahColors.Error.copy(alpha = 0.22f)
                    WordStatus.READING -> AyahColors.Reading.copy(alpha = 0.5f)
                    WordStatus.SKIPPED -> AyahColors.Error.copy(alpha = 0.12f)
                    else -> null
                }
                if (bg != null) addStyle(SpanStyle(background = bg), s0, end)
                if (selectedIndex == j) {
                    addStyle(
                        SpanStyle(
                            background = AyahColors.Primary.copy(alpha = 0.42f),
                            color = AyahColors.Primary,
                        ),
                        s0,
                        end,
                    )
                }
            }
        }
    }
}

/** Metadata teks mengalir satu surah: teks penuh + offset global per ayat/kata. */
private data class FlowMeta(
    val fullText: String,
    val ayahStart: IntArray,
    val ayahEnd: IntArray,
    val sajdah: BooleanArray,
    val wordStarts: List<IntArray>,
    val wordsByAyah: List<List<String>>,
)

private fun buildFlowMeta(ayahs: List<MushafAyah>): FlowMeta {
    val sb = StringBuilder()
    val ayahStart = IntArray(ayahs.size)
    val ayahEnd = IntArray(ayahs.size)
    val sajdah = BooleanArray(ayahs.size)
    val wordStarts = ArrayList<IntArray>(ayahs.size)
    val wordsByAyah = ArrayList<List<String>>(ayahs.size)
    ayahs.forEachIndexed { i, entry ->
        ayahStart[i] = sb.length
        val tokens = org.opennur.tahsin.data.quran.Ayah(entry.number, entry.text).words
        wordsByAyah.add(tokens)
        val ws = IntArray(tokens.size)
        var searchFrom = 0
        tokens.forEachIndexed { j, w ->
            val idx = entry.text.indexOf(w, searchFrom)
            ws[j] = ayahStart[i] + if (idx >= 0) {
                searchFrom = idx + w.length
                idx
            } else {
                searchFrom
            }
        }
        wordStarts.add(ws)
        sb.append(entry.text)
        ayahEnd[i] = sb.length
        sajdah[i] = entry.isSajdah
        // Separator selebar badge: 2 em-space (4 kalau ada badge sujud juga) —
        // badge nomor & sujud digambar di area ini, tidak menimpa huruf.
        sb.append(if (entry.isSajdah) "\u2003\u2003\u2003\u2003" else "\u2003\u2003")
    }
    return FlowMeta(sb.toString(), ayahStart, ayahEnd, sajdah, wordStarts, wordsByAyah)
}

/** Ayah ke berapa yang memuat offset; separator/badge milik ayat sebelumnya. */
private fun ayahIndexAt(offset: Int, starts: IntArray, ends: IntArray): Int {
    var result = -1
    for (i in starts.indices) {
        if (offset >= starts[i]) result = i else break
    }
    return result
}

