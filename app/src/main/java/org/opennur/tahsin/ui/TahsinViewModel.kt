package org.opennur.tahsin.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.ComposedPage
import org.opennur.tahsin.data.quran.MushafPageComposer
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.quran.AssetQuranRepository
import org.opennur.tahsin.data.quran.Surah
import org.opennur.tahsin.data.tajwid.TajwidEngine
import org.opennur.tahsin.data.tajwid.TajwidRule
import org.opennur.tahsin.data.vocab.VocabEntry
import org.opennur.tahsin.data.vocab.VocabularyEngine
import org.opennur.tahsin.data.vocab.VocabularyRepository
import org.opennur.tahsin.data.vocab.MorphologyEngine
import org.opennur.tahsin.data.vocab.RootInfo
import org.opennur.tahsin.stt.ArabicSpeechRecognizer
import org.opennur.tahsin.stt.AlignedWord
import org.opennur.tahsin.stt.TranscriptAligner
import org.opennur.tahsin.stt.WordStatus
import org.opennur.tahsin.theme.ArabicFont
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.ui.AppStrings
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Bookmark
import org.opennur.tahsin.util.BookmarkStore
import org.opennur.tahsin.util.ReadingHistoryStore
import org.opennur.tahsin.util.AudioDownloader
import org.opennur.tahsin.util.DownloadProgress
import org.opennur.tahsin.util.DownloadService
import org.opennur.tahsin.util.FontScales
import org.opennur.tahsin.util.FontStore
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationEvents
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.ReadingStats
import org.opennur.tahsin.util.AyahStats
import org.opennur.tahsin.util.PlaySource
import org.opennur.tahsin.util.ReadingStatsStore
import org.opennur.tahsin.util.Reciter
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.TahsinAudioPlayer
import org.opennur.tahsin.widget.StreakReminderAlarm
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** State layar utama Tahsin. */
sealed interface TahsinUiState {
    data object Loading : TahsinUiState

    data class Error(val message: String) : TahsinUiState

    data class Ready(
        val surahs: List<Surah> = emptyList(),
        /** Paginasi mushaf Madani (604 halaman) — dari assets/quran/pages.json. */
        val pagination: MushafPagination = MushafPagination(0, 0, emptyList(), emptyList()),
        /** Halaman aktif 0-based — navigasi seperti membalik halaman mushaf. */
        val pageIndex: Int = 0,
        val pageCount: Int = 1,
        /** Halaman tersusun siap-render (null selama konten surah di halaman dimuat). */
        val composedPage: ComposedPage? = null,
        /** Terjemahan tersembunyi secara default (mushaf asli); toggle di header. */
        val showTranslation: Boolean = false,
        /** Surah & ayat AKTIF = sasaran latihan STT (bukan navigasi). */
        val surahNumber: Int = 1,
        val ayahIndex: Int = 0,
        /** Bookmark ayat (surah, ayah) yang disimpan pengguna. */
        val bookmarks: Set<Bookmark> = emptySet(),
        /** Isi surah sedang dimuat (diunduh dari equran.id). */
        val loadingSurah: Boolean = false,
        val listening: Boolean = false,
        val transcript: String = "",
        val alignedWords: List<AlignedWord> = emptyList(),
        val issues: List<ReadingIssue> = emptyList(),
        /** Riwayat bacaan ayat aktif (dari ReadingStatsStore) — untuk info cepat. */
        val ayahStats: AyahStats? = null,
        val selectedWordIndex: Int? = null,
        val selectedWordRules: List<TajwidRule> = emptyList(),
        /** Arti kata terpilih (bahasa aktif) — null kalau belum terkurasi. */
        val selectedWordMeaning: String? = null,
        /** Info akar kata terpilih — null kalau kata tidak dikenal/tidak punya akar. */
        val selectedWordRoot: RootInfo? = null,
        val message: String? = null,
        val fontScale: Float = 1.5f,
        val language: AppLanguage = AppLanguage.ID,
        val arabicFont: ArabicFont = ArabicFont.UTSMANI,
        /** FontFamily efektif (font file kalau ada, fallback sistem). */
        val arabicFontFamily: FontFamily = FontFamily.Default,
        val darkMode: Boolean = false,
        /** Pewarnaan huruf tajwid di mushaf (gaya mushaf tajwid). */
        val tajwidColor: Boolean = true,
        /** Mode pemutaran audio: ayat ini saja / lanjut terus / ulang terus. */
        val audioMode: AudioPlaybackMode = AudioPlaybackMode.AYAH,
        /** Qari' (perawi) audio ayat aktif. */
        val reciter: Reciter = Reciter.MINSHAWY,
        /** Kecepatan pemutaran audio (0.5×–1.25×). */
        val audioSpeed: Float = 1.0f,
        /** Notifikasi harian "Ayah of the Day" (toggle di drawer). */
        val ayahOfDayEnabled: Boolean = true,
        /** Pengingat harian untuk menjaga streak (toggle di Pengaturan). */
        val streakReminderEnabled: Boolean = false,
        /** Sedang memutar audio (untuk tombol Dengar/Stop). */
        /** Audio ayat sedang diputar (tombol Dengar/Stop di footer). */
        val isAudioPlaying: Boolean = false,
        /** Audio kata sedang diputar (tombol Stop di tooltip kata). */
        val isWordPlaying: Boolean = false,
        /** Petunjuk geser masih tampil (belum ditutup user). */
        val showSwipeHint: Boolean = true,
        /** Sedang mengunduh audio (agregat semua surah, tampil di atas tombol). */
        val isDownloading: Boolean = false,
        val downloadDone: Int = 0,
        val downloadTotal: Int = 0,
        /** Bulk unduh semua surah sedang berjalan (penjaga agar tidak bertumpuk). */
        val isDownloadingAll: Boolean = false,
        /** Popup keterangan saat unduhan audio dimulai. */
        val showDownloadNotice: Boolean = false,
        /** Prompt izin unduhan latar belakang (belum pernah ditanya). */
        val showBackgroundPrompt: Boolean = false,
    ) : TahsinUiState {
        val surah: Surah? get() = surahs.find { it.number == surahNumber }
        /** Apakah ayat aktif sedang di-bookmark. */
        val bookmarked: Boolean get() = Bookmark(surahNumber, ayahIndex + 1) in bookmarks
        val ayah: Ayah? get() = surah?.ayahs?.getOrNull(ayahIndex)
    }
}

/** Satu temuan kesalahan bacaan (kata tidak cocok / terlewat). */
data class ReadingIssue(
    val wordIndex: Int,
    val word: String,
    val spoken: String?,
    val rules: List<TajwidRule>,
)

/** Target buka dari widget/notifikasi "Ayah of the Day" — deliveryId dibuat saat
 * pengiriman (bukan dibekukan di PendingIntent) supaya ketukan berulang tetap memicu. */
data class OpenTarget(
    val surah: Int,
    val ayah: Int,
    val deliveryId: Long,
)

/** State setelan ringkas lintas layar — tidak bergantung data mushaf. Dipakai
 * portal Home & layar Pengaturan; disinkronkan dari [TahsinUiState.Ready]. */
data class SettingsUiState(
    val language: AppLanguage = AppLanguage.ID,
    val darkMode: Boolean = false,
    val tajwidColor: Boolean = true,
    val showTranslation: Boolean = false,
    val reciter: Reciter = Reciter.MINSHAWY,
    val audioSpeed: Float = 1.0f,
    val ayahOfDayEnabled: Boolean = true,
    val streakReminderEnabled: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadDone: Int = 0,
    val downloadTotal: Int = 0,
    val showDownloadNotice: Boolean = false,
    val showBackgroundPrompt: Boolean = false,
)

/**
 * Mode pemutaran audio mushaf (tombol di samping "Dengar").
 * - AYAH: putar ayat aktif sekali lalu berhenti.
 * - CONTINUOUS: lanjut otomatis ke ayat berikutnya (seperti membaca terus).
 * - REPEAT: ulangi ayat aktif terus-menerus.
 */
enum class AudioPlaybackMode { AYAH, CONTINUOUS, REPEAT }

@HiltViewModel
@Suppress("LargeClass")
class TahsinViewModel @Inject constructor(
    private val app: Context,
    private val repository: QuranRepository,
    private val speech: ArabicSpeechRecognizer,
    private val audioPlayer: TahsinAudioPlayer,
    private val settings: SettingsStore,
    private val downloader: AudioDownloader,
    private val fontStore: FontStore,
    private val statsStore: ReadingStatsStore,
    private val bookmarkStore: BookmarkStore,
    private val vocabulary: VocabularyRepository,
    private val readingHistory: ReadingHistoryStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TahsinUiState>(TahsinUiState.Loading)
    val uiState: StateFlow<TahsinUiState> = _uiState.asStateFlow()

    /** Kosakata terkurasi (cache) untuk arti kata di tooltip; dimuat di reload(). */
    @Volatile
    private var vocabEntries: List<VocabEntry> = emptyList()

    /** Catat kunjungan ayat ke riwayat baca (IO; dedup di store). */
    private fun recordReading(surah: Int, ayahNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            readingHistory.record(surah, ayahNumber, System.currentTimeMillis())
        }
    }

    /** State setelan lintas layar (portal Home & Pengaturan) — diinisialisasi
     * dari penyimpanan, lalu disinkronkan via [syncSettings] tiap state berubah. */
    private val _settingsState = MutableStateFlow(
        SettingsUiState(
            language = currentLanguage(),
            darkMode = settings.darkMode,
            tajwidColor = settings.tajwidColor,
            showTranslation = settings.showTranslation,
            reciter = settings.reciter,
            audioSpeed = settings.audioSpeed,
            ayahOfDayEnabled = settings.ayahOfDayEnabled,
            streakReminderEnabled = settings.streakReminderEnabled,
        ),
    )
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    /** Unduhan aktif per surah (mendukung beberapa surah sekaligus). */
    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<Int, Job>()
    private val activeTotals = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    private val activeDone = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    /** Callback play yang menunggu unduhan surah selesai. */
    private val pendingCallbacks = mutableMapOf<Int, MutableList<() -> Unit>>()

    /** Mode lanjut (audio) menunggu halaman berikutnya termuat untuk diputar. */
    private var pendingAutoPlay = false
    /** Menunggu halaman berikutnya siap sebelum melanjutkan sesi flow. */
    private var pendingFlowStart = false
    private var flowRestartJob: Job? = null
    private var recitationFlowActive = false
    private var recitationGeneration = 0L
    private var accumulatedTranscript = ""
    private var accumulatedTarget: Pair<Int, Int>? = null

    private companion object {
        const val FLOW_RESTART_DELAY_MS = 250L
        const val FLOW_READY_RETRY_DELAY_MS = 100L
        const val FLOW_READY_RETRIES = 30
        val RETRYABLE_SPEECH_ERRORS = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        )
    }
    /** Target buka dari widget/notifikasi "Ayah of the Day" (surah, ayat 1-based). */
    private var pendingOpenAt: Pair<Int, Int>? = null
    /** Penjaga muatan halaman: geser cepat → muatan lama yang selesai belakangan dibuang. */
    private var pageLoadGeneration = 0
    /** Stop/start putar: putar yang masih menunggu unduhan dibatalkan saat berubah. */
    private var playGeneration = 0

    init {
        AyahColors.isDark = settings.darkMode
        audioPlayer.onPlaybackChange = { playing ->
            // Sumber pemutaran (AYAH/WORD) ditentukan PLAYER — bukan flag lokal,
            // jadi tidak ada race saat Dengar & Kata ditekan bersamaan.
            updateReady { it.copy(
                isAudioPlaying = playing && audioPlayer.source == PlaySource.AYAH,
                isWordPlaying = playing && audioPlayer.source == PlaySource.WORD,
            ) }
        }
        // Audio selesai natural → lanjut/ulang sesuai mode pemutaran.
        audioPlayer.onCompletion = { onAudioCompleted() }
        reload()
    }

    fun reload() {
        _uiState.value = TahsinUiState.Loading
        _uiState.value = try {
            val pagination = repository.pagination()
            val pageCount = pagination.pageCount.coerceAtLeast(1)
            val startPage = ((pagination.pageOf(settings.surahNumber, settings.ayahIndex + 1) ?: 1) - 1)
                .coerceIn(0, pageCount - 1)
            TahsinUiState.Ready(
                surahs = repository.surahList(),
                pagination = pagination,
                pageIndex = startPage,
                pageCount = pageCount,
                surahNumber = settings.surahNumber,
                ayahIndex = settings.ayahIndex,
                loadingSurah = true,
                fontScale = settings.fontScale,
                language = currentLanguage(),
                arabicFont = ArabicFont.UTSMANI,
                arabicFontFamily = fontStore.loadFamily(ArabicFont.UTSMANI),
                darkMode = settings.darkMode,
                tajwidColor = settings.tajwidColor,
                audioMode = currentAudioMode(),
                reciter = settings.reciter,
                audioSpeed = settings.audioSpeed,
                ayahOfDayEnabled = settings.ayahOfDayEnabled,
                streakReminderEnabled = settings.streakReminderEnabled,
                showSwipeHint = !settings.swipeHintDismissed,
                showTranslation = settings.showTranslation,
            )
        } catch (e: Exception) {
            TahsinUiState.Error(e.message ?: AppStrings.of(currentLanguage()).msgMushafLoadFailed)
        }
        syncSettings()
        // Unduh font default (Utsmani/Amiri) otomatis kalau file belum ada.
        val activeFont = ArabicFont.UTSMANI
        if (activeFont.downloadUrl != null && !fontStore.fileExists(activeFont)) {
            viewModelScope.launch {
                runCatching { fontStore.ensureFont(activeFont) }
                updateReady { it.copy(arabicFontFamily = fontStore.loadFamily(activeFont)) }
            }
        }
        // Muat konten surah pada halaman terakhir yang dibuka (default: Al-Fatihah).
        (currentReady())?.let { ensurePageLoaded(it.pageIndex) }
        // Muat bookmark ayat.
        viewModelScope.launch {
            updateReady { it.copy(bookmarks = bookmarkStore.load()) }
        }
        // Muat daftar kosakata terkurasi untuk arti kata di tooltip.
        viewModelScope.launch(Dispatchers.IO) {
            vocabEntries = vocabulary.curatedEntries()
            MorphologyEngine.init(vocabEntries)
        }
        // Target dari widget/notifikasi (state baru saja siap) → buka langsung.
        pendingOpenAt?.let { (s, a) -> openAt(s, a) }
        // Proses yang mati saat mengunduh meninggalkan manifest + `.part`; lanjutkan
        // saat aplikasi dibuka lagi, tanpa meminta izin latar belakang sekali lagi.
        resumePendingDownloads()
    }

    /** Tutup petunjuk geser (persisten — tidak muncul lagi setelah restart). */
    fun dismissSwipeHint() {
        settings.swipeHintDismissed = true
        updateReady { it.copy(showSwipeHint = false) }
    }

    // ---- navigasi HALAMAN mushaf (bukan per ayat) ----

    /** Halaman berikutnya (alur RTL: geser kiri) — seperti membalik mushaf. */
    fun nextPage() {
        val s = currentReady() ?: return
        if (s.pageIndex < s.pageCount - 1) moveToPage(s.pageIndex + 1)
    }

    /** Halaman sebelumnya (alur RTL: geser kanan). */
    fun prevPage() {
        val s = currentReady() ?: return
        if (s.pageIndex > 0) moveToPage(s.pageIndex - 1)
    }

    /** Pager digeser ke halaman [index] — muat konten & set ayat aktif (ayat pertama). */
    fun selectPage(index: Int) {
        if (recitationFlowActive) stopRecitation()
        moveToPage(index)
    }

    /** Lompat ke halaman pertama surah [number]. */
    fun jumpToSurah(number: Int) {
        if (recitationFlowActive) stopRecitation()
        pendingAutoPlay = false // navigasi manual membatalkan rantai audio tertunda
        // Navigasi manual membatalkan target widget/notifikasi yang belum terpakai.
        pendingOpenAt = null
        val s = currentReady() ?: return
        val page = s.pagination.firstPageOf(number) ?: return
        moveToPage(page - 1, activeSurah = number, activeAyahNumber = 1)
    }

    /** Lompat ke halaman awal juz [juz] (1..30). */
    fun jumpToJuz(juz: Int) {
        if (recitationFlowActive) stopRecitation()
        pendingAutoPlay = false
        pendingOpenAt = null
        val s = currentReady() ?: return
        val start = s.pagination.juzStarts.firstOrNull { it.juz == juz } ?: return
        val page = s.pagination.pageOf(start.surah, start.ayah) ?: return
        moveToPage(page - 1, activeSurah = start.surah, activeAyahNumber = start.ayah)
    }

    /** Lompat langsung ke halaman [page] (1-based, 1..604) — navigasi utama mushaf. */
    fun jumpToPage(page: Int) {
        if (recitationFlowActive) stopRecitation()
        pendingAutoPlay = false
        pendingOpenAt = null
        val s = currentReady() ?: return
        if (page !in 1..s.pageCount) return
        moveToPage(page - 1)
    }

    /**
     * Pindah ke halaman [index] (0-based). [activeSurah]/[activeAyahNumber]
     * menentukan ayat aktif (sasaran latihan STT); default: ayat pertama halaman.
     */
    private fun moveToPage(
        index: Int,
        activeSurah: Int? = null,
        activeAyahNumber: Int? = null,
    ) {
        val s = currentReady() ?: return
        val clamped = index.coerceIn(0, s.pageCount - 1)
        val first = s.pagination.firstAyahOf(clamped + 1)
        val surah = activeSurah ?: first?.first ?: s.surahNumber
        val ayahNumber = activeAyahNumber ?: first?.second ?: 1
        settings.surahNumber = surah
        settings.ayahIndex = ayahNumber - 1
        updateReady { it.copy(
            pageIndex = clamped,
            surahNumber = surah,
            ayahIndex = ayahNumber - 1,
            ayahStats = null,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            selectedWordMeaning = null,
            message = null,
        ) }
        refreshAyahStats(surah, ayahNumber)
        ensurePageLoaded(clamped)
        recordReading(surah, ayahNumber)
    }

    /**
     * Pastikan konten surah pada halaman [index] (+ surah pertama halaman
     * berikutnya untuk pra-muat) tersedia, lalu susun ulang halaman.
     */
    private fun ensurePageLoaded(index: Int, force: Boolean = false) {
        val s = currentReady() ?: return
        val page = s.pagination.pages.getOrNull(index) ?: return
        val currentNeeded = page.segments.map { it.surah }.toSet()
        // Praload BACKGROUND halaman sebelum & sesudah (biar swipe mulus tanpa
        // kilat "memuat surah"): semua surah di halaman index-1..index+1.
        val neighborNeeded = buildSet {
            for (i in index - 1..index + 1) {
                if (i in 0 until s.pageCount) {
                    val p = s.pagination.pages[i]
                    addAll(p.segments.map { it.surah })
                }
            }
        } - currentNeeded
        // force=true (ganti bahasa): surah yang sudah dimuat tetap dimuat ulang
        // supaya terjemahan mengikuti bahasa yang baru.
        val loaded = { n: Int -> s.surahs.firstOrNull { it.number == n }?.ayahs?.isEmpty() != false }
        val currentMissing = currentNeeded.filter { force || loaded(it) }
        val neighborMissing = neighborNeeded.filter { force || loaded(it) }
        val gen = ++pageLoadGeneration
        if (currentMissing.isEmpty() && neighborMissing.isEmpty()) {
            refreshCurrentPage(gen)
            return
        }
        // Indikator loading HANYA kalau konten halaman AKTIF yang belum ada —
        // praload tetangga berjalan sunyi di background.
        if (currentMissing.isNotEmpty()) updateReady { it.copy(loadingSurah = true) }
        viewModelScope.launch {
            val lang = currentLanguage()
            (currentMissing + neighborMissing).forEach { number ->
                val cached = runCatching { repository.cachedSurah(number, lang) }.getOrNull()
                val loadedSurah = cached ?: runCatching { repository.fetchSurah(number, lang) }.getOrNull()
                if (loadedSurah != null) {
                    updateReady { st ->
                        st.copy(surahs = st.surahs.map { x -> if (x.number == loadedSurah.number) loadedSurah else x })
                    }
                } else if (number in currentMissing) {
                    updateReady { st ->
                        st.copy(message = "${AppStrings.of(lang).msgSurahLoadFailed} $number: ${AppStrings.of(lang).msgCheckConnection}")
                    }
                }
            }
            refreshCurrentPage(gen)
        }
    }

    /** Susun ulang halaman aktif — hanya untuk generasi muatan terbaru. */
    private fun refreshCurrentPage(gen: Int = pageLoadGeneration) {
        if (gen != pageLoadGeneration) return // muatan halaman lama dibuang
        val s = currentReady() ?: return
        val contents = s.surahs.associateBy { it.number }
        val composed = MushafPageComposer.composePage(s.pagination, s.pageIndex + 1, contents)
        updateReady { it.copy(
            loadingSurah = false,
            composedPage = composed,
        ) }
        refreshAyahStats(s.surahNumber, s.ayah?.number ?: 1)
        // Mode audio lanjut lintas halaman: putar ayat pertama begitu konten siap.
        if (pendingAutoPlay) {
            pendingAutoPlay = false
            playAyahNow()
        }
        if (pendingFlowStart) {
            pendingFlowStart = false
            scheduleFlowStart(recitationGeneration)
        }
    }

    /** Ayat aktif berikutnya dalam halaman; habis → halaman berikutnya. */
    fun nextAyah() {
        val s = currentReady() ?: return
        val page = s.composedPage ?: return
        val pos = page.ayahs.indexOfFirst { it.surah == s.surahNumber && it.number == s.ayahIndex + 1 }
        if (pos >= 0 && pos < page.ayahs.lastIndex) {
            val next = page.ayahs[pos + 1]
            setActiveAyah(next.surah, next.number)
            return
        }
        if (s.pageIndex < s.pageCount - 1) moveToPage(s.pageIndex + 1)
    }

    /** Ayat aktif sebelumnya dalam halaman; di awal halaman → halaman sebelumnya. */
    fun prevAyah() {
        val s = currentReady() ?: return
        val page = s.composedPage ?: return
        val pos = page.ayahs.indexOfFirst { it.surah == s.surahNumber && it.number == s.ayahIndex + 1 }
        if (pos > 0) {
            val prev = page.ayahs[pos - 1]
            setActiveAyah(prev.surah, prev.number)
            return
        }
        if (s.pageIndex > 0) {
            val prevPage = s.pagination.pages.getOrNull(s.pageIndex - 1)
            val last = prevPage?.segments?.lastOrNull()
            if (last != null) {
                moveToPage(s.pageIndex - 1, activeSurah = last.surah, activeAyahNumber = last.toAyah)
            } else {
                moveToPage(s.pageIndex - 1)
            }
        }
    }

    /** Ketuk ayat di halaman → jadikan ayat aktif (sasaran latihan). */
    fun selectAyahAt(surah: Int, ayahNumber: Int) {
        if (recitationFlowActive) stopRecitation()
        setActiveAyah(surah, ayahNumber)
    }

    /** Jadikan surah:ayat sebagai ayat aktif tanpa pindah halaman. */
    private fun setActiveAyah(surah: Int, ayahNumber: Int) {
        val s = currentReady() ?: return
        // Ayat dari dropdown [Ayat] bisa berada di HALAMAN lain — mushaf ikut
        // pindah ke halaman yang memuat ayat tersebut (ketukan di halaman yang
        // sama tidak perlu memindahkan halaman).
        val page = s.pagination.pageOf(surah, ayahNumber)
        if (page != null && page - 1 != s.pageIndex) {
            moveToPage(page - 1, activeSurah = surah, activeAyahNumber = ayahNumber)
            return
        }
        settings.surahNumber = surah
        settings.ayahIndex = ayahNumber - 1
        updateReady { it.copy(
            surahNumber = surah,
            ayahIndex = ayahNumber - 1,
            ayahStats = null,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            selectedWordMeaning = null,
            message = null,
        ) }
        refreshAyahStats(surah, ayahNumber)
        recordReading(surah, ayahNumber)
    }

    /** Tampilkan/sembunyikan terjemahan di bawah mushaf (default: tersembunyi). */
    fun toggleTranslation() {
        val next = !settings.showTranslation
        settings.showTranslation = next
        updateReady { it.copy(showTranslation = next) }
    }

    /**
     * Ubah ukuran huruf mushaf (tombol A− / A+): di-clamp ke rentang yang
     * aman, disimpan ke settings supaya bertahan saat app ditutup.
     */
    fun setFontScale(value: Float) {
        val next = FontScales.clamp(value)
        settings.fontScale = next
        updateReady { it.copy(fontScale = next) }
    }

    // ---- buka target (widget "Ayah of the Day" / notifikasi) ----

    /** Buka surah/ayat tertentu (halaman yang memuatnya); aman dipanggil kapan saja. */
    fun openAt(surahNumber: Int, ayahNumber: Int) {
        if (currentReady() == null) {
            pendingOpenAt = surahNumber to ayahNumber
            return
        }
        applyOpenAt(surahNumber, ayahNumber)
    }

    private fun applyOpenAt(surahNumber: Int, ayahNumber: Int) {
        pendingOpenAt = null
        val s = currentReady() ?: return
        val page = s.pagination.pageOf(surahNumber, ayahNumber) ?: return
        moveToPage(page - 1, activeSurah = surahNumber, activeAyahNumber = ayahNumber)
    }

    /** Nyalakan/matikan notifikasi harian "Ayah of the Day" (toggle di Pengaturan). */
    fun toggleAyahOfDay() {
        val next = !settings.ayahOfDayEnabled
        settings.ayahOfDayEnabled = next
        _settingsState.update { it.copy(ayahOfDayEnabled = next) }
        updateReady { it.copy(ayahOfDayEnabled = next) }
    }

    /** Nyalakan/matikan pengingat streak harian (toggle di Pengaturan). */
    fun toggleStreakReminder() {
        val next = !settings.streakReminderEnabled
        settings.streakReminderEnabled = next
        _settingsState.update { it.copy(streakReminderEnabled = next) }
        updateReady { it.copy(streakReminderEnabled = next) }
        if (next) StreakReminderAlarm.scheduleDaily(app) else StreakReminderAlarm.cancel(app)
    }

    // ---- mic / STT ----

    fun toggleMic() {
        val s = currentReady() ?: return
        if (s.listening || recitationFlowActive) {
            stopRecitation()
            return
        }
        pendingAutoPlay = false // inisiatif manual membatalkan rantai audio tertunda
        val ayah = s.ayah ?: return
        val words = ayah.words
        if (words.isEmpty()) return
        recitationFlowActive = true
        recitationGeneration++
        GamificationEvents.beginSuppression()
        startListeningSession(s.surahNumber, ayah.number, words, recitationGeneration, resetProgress = true)
    }

    private fun startListeningSession(
        surahNumber: Int,
        ayahNumber: Int,
        words: List<String>,
        generation: Long,
        resetProgress: Boolean,
    ) {
        if (generation != recitationGeneration || words.isEmpty()) return
        val target = surahNumber to ayahNumber
        if (resetProgress || accumulatedTarget != target) {
            accumulatedTranscript = ""
            accumulatedTarget = target
        }
        val displayedTranscript = accumulatedTranscript
        updateReady { it.copy(
            listening = true,
            transcript = displayedTranscript,
            alignedWords = if (displayedTranscript.isBlank()) {
                emptyList()
            } else {
                TranscriptAligner.align(displayedTranscript, words)
            },
            issues = if (displayedTranscript.isBlank()) emptyList() else issuesFor(displayedTranscript, words),
            message = null,
        ) }
        speech.start(object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) {
                if (isCurrentRecitation(surahNumber, ayahNumber, generation)) {
                    onTranscript(TranscriptAligner.appendTranscript(accumulatedTranscript, text), words)
                }
            }

            override fun onResult(text: String) {
                if (!isCurrentRecitation(surahNumber, ayahNumber, generation)) return
                accumulatedTranscript = TranscriptAligner.appendTranscript(accumulatedTranscript, text)
                onTranscript(accumulatedTranscript, words)
                val complete = TranscriptAligner.reachesEnd(currentReady()?.alignedWords.orEmpty())
                if (!complete) {
                    scheduleFlowRestart(generation)
                    return
                }
                recordAttempt(accumulatedTranscript, words)
                maybePlayFeedbackTone()
                if (recitationFlowActive) advanceRecitation(generation) else updateReady {
                    it.copy(listening = false)
                }
            }

            override fun onError(error: Int) {
                if (!isCurrentRecitation(surahNumber, ayahNumber, generation)) return
                if (recitationFlowActive && error in RETRYABLE_SPEECH_ERRORS) {
                    scheduleFlowRestart(generation)
                } else {
                    stopRecitation(AppStrings.sttErrorMessage(error, currentLanguage()))
                }
            }

            override fun onListeningChanged(listening: Boolean) {
                if (generation != recitationGeneration) return
                // SpeechRecognizer ends each short session before onResults; keep
                // the footer active while flow schedules the next one.
                if (!listening && recitationFlowActive) return
                updateReady { it.copy(listening = listening) }
            }
        })
    }

    private fun isCurrentRecitation(surah: Int, ayah: Int, generation: Long): Boolean {
        val s = currentReady() ?: return false
        return generation == recitationGeneration && s.surahNumber == surah && s.ayah?.number == ayah
    }

    private fun scheduleFlowRestart(generation: Long, resetProgress: Boolean = false) {
        flowRestartJob?.cancel()
        flowRestartJob = viewModelScope.launch {
            repeat(FLOW_READY_RETRIES) { attempt ->
                delay(if (attempt == 0) FLOW_RESTART_DELAY_MS else FLOW_READY_RETRY_DELAY_MS)
                val s = currentReady() ?: return@launch
                if (!recitationFlowActive || generation != recitationGeneration) return@launch
                val ayah = s.ayah
                if (ayah != null && ayah.words.isNotEmpty()) {
                    startListeningSession(s.surahNumber, ayah.number, ayah.words, generation, resetProgress)
                    return@launch
                }
                if (!s.loadingSurah) {
                    stopRecitation(s.message ?: AppStrings.of(currentLanguage()).msgMushafLoadFailed)
                    return@launch
                }
            }
            stopRecitation(AppStrings.of(currentLanguage()).msgMushafLoadFailed)
        }
    }

    private fun scheduleFlowStart(generation: Long) = scheduleFlowRestart(generation, resetProgress = true)

    private fun advanceRecitation(generation: Long) {
        if (!recitationFlowActive || generation != recitationGeneration) return
        val s = currentReady() ?: return
        val page = s.composedPage ?: run {
            stopRecitation(AppStrings.of(currentLanguage()).msgMushafLoadFailed)
            return
        }
        val pos = page.ayahs.indexOfFirst { it.surah == s.surahNumber && it.number == s.ayahIndex + 1 }
        if (pos >= 0 && pos < page.ayahs.lastIndex) {
            val next = page.ayahs[pos + 1]
            setActiveAyah(next.surah, next.number)
            scheduleFlowStart(generation)
            return
        }
        if (s.pageIndex < s.pageCount - 1) {
            pendingFlowStart = true
            moveToPage(s.pageIndex + 1)
            return
        }
        stopRecitation(AppStrings.of(currentLanguage()).msgMurojaahDone)
    }

    private fun stopRecitation(message: String? = null) {
        recitationFlowActive = false
        recitationGeneration++
        pendingFlowStart = false
        flowRestartJob?.cancel()
        flowRestartJob = null
        accumulatedTranscript = ""
        accumulatedTarget = null
        speech.stop()
        GamificationEvents.endSuppression()
        updateReady { it.copy(listening = false, message = message ?: it.message) }
    }

    // ---- bookmark ayat ----

    /** Tambah/hapus bookmark untuk ayat aktif; persisten di disk. */
    fun toggleBookmark() {
        val s = currentReady() ?: return
        val target = Bookmark(s.surahNumber, s.ayahIndex + 1)
        viewModelScope.launch {
            val updated = bookmarkStore.toggle(target)
            updateReady { it.copy(bookmarks = updated) }
        }
    }

    /**
     * Muat ulang bookmark dari disk — dipanggil tiap layar Tahsin masuk
     * komposisi (LaunchedEffect di TahsinScreen). Bookmark bisa berubah dari
     * layar Ayat Favorit (VM berbeda), jadi jangan hanya andalkan init.
     */
    fun refreshBookmarks() {
        viewModelScope.launch {
            updateReady { it.copy(bookmarks = bookmarkStore.load()) }
        }
    }

    // ---- mode pemutaran audio (tombol di samping "Dengar") ----

    /** Ubah mode pemutaran: ayat ini saja / lanjut otomatis / ulang terus. */
    fun setAudioMode(mode: AudioPlaybackMode) {
        settings.audioMode = mode.name
        updateReady { it.copy(audioMode = mode) }
    }

    private fun currentAudioMode(): AudioPlaybackMode =
        AudioPlaybackMode.entries.firstOrNull { it.name == settings.audioMode }
            ?: AudioPlaybackMode.AYAH

    // ---- qari' & kecepatan audio ----

    /** Ganti qari' (perawi) audio ayat; berlaku untuk unduhan & pemutaran berikutnya. */
    fun setReciter(reciter: Reciter) {
        settings.reciterSlug = reciter.slug
        _settingsState.update { it.copy(reciter = reciter) }
        updateReady { it.copy(reciter = reciter) }
    }

    /** Ganti kecepatan pemutaran; langsung berlaku kalau sedang memutar. */
    fun setAudioSpeed(speed: Float) {
        settings.audioSpeed = speed
        audioPlayer.applySpeed(speed)
        _settingsState.update { it.copy(audioSpeed = settings.audioSpeed) }
        updateReady { it.copy(audioSpeed = settings.audioSpeed) }
    }

    /**
     * Dipanggil saat hasil final STT tiba. Kalau mode flow nyala dan SELURUH
     * selesai — lanjut/ulang sesuai mode pemutaran (bukan mode flow lagi).
     */
    private fun onAudioCompleted() {
        // Hanya untuk audio AYAH — pemutaran kata (tooltip) tidak ikut rantai.
        if (audioPlayer.source != PlaySource.AYAH) return
        when (currentReady()?.audioMode ?: return) {
            AudioPlaybackMode.REPEAT -> playAyahNow()
            AudioPlaybackMode.CONTINUOUS -> advanceAudioToNextAyah()
            AudioPlaybackMode.AYAH -> Unit
        }
    }

    /** Mode lanjut: pindah ke ayat berikutnya (dalam halaman, atau halaman berikutnya). */
    private fun advanceAudioToNextAyah() {
        val s = currentReady() ?: return
        val page = s.composedPage ?: return
        val pos = page.ayahs.indexOfFirst { it.surah == s.surahNumber && it.number == s.ayahIndex + 1 }
        if (pos >= 0 && pos < page.ayahs.lastIndex) {
            val next = page.ayahs[pos + 1]
            setActiveAyah(next.surah, next.number)
            playAyahNow()
            return
        }
        if (s.pageIndex < s.pageCount - 1) {
            pendingAutoPlay = true
            moveToPage(s.pageIndex + 1)
            return
        }
        updateReady { it.copy(message = AppStrings.of(currentLanguage()).msgMurojaahDone) }
    }

    // ---- umpan suara muroja'ah (bisa muroja'ah tanpa melihat layar) ----

    private var toneGen: ToneGenerator? = null

    private fun toneGenerator(): ToneGenerator? {
        if (toneGen == null) {
            toneGen = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            } catch (e: Exception) {
                null
            }
        }
        return toneGen
    }

    /** Beep pendek nanjak — semua kata ayat sudah benar. */
    private fun playSuccessTone() {
        try {
            toneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (_: Exception) {
        }
    }

    /**
     * Umpan gagal yang JELAS: dua nada NACK berurutan + getar — biar terasa
     * walau tidak melihat layar.
     */
    private fun playErrorFeedback() {
        val gen = toneGenerator()
        if (gen != null) {
            try {
                gen.startTone(ToneGenerator.TONE_PROP_NACK, 260)
            } catch (_: Exception) {
            }
        }
        vibrateError()
        viewModelScope.launch {
            delay(340)
            try {
                toneGenerator()?.startTone(ToneGenerator.TONE_PROP_NACK, 260)
            } catch (_: Exception) {
            }
        }
    }

    /** Getar singkat dua kali (pola buzz-buzz) untuk indikasi gagal. */
    private fun vibrateError() {
        runCatching {
            // getSystemService(Class) tidak deprecated (API 23+; minSdk 26).
            val vib = app.getSystemService(Vibrator::class.java) ?: return
            if (!vib.hasVibrator()) return
            val pattern = longArrayOf(0, 160, 80, 160)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        }
    }

    /** Saat hasil final: ada kata salah → umpan gagal; sempurna → umpan sukses. */
    private fun maybePlayFeedbackTone() {
        val s = currentReady() ?: return
        val aligned = s.alignedWords
        if (aligned.isEmpty()) return
        val hasError = aligned.any {
            it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED
        }
        if (hasError) playErrorFeedback() else playSuccessTone()
    }

    /**
     * Muat riwayat ayat aktif dari penyimpanan di thread IO; hasilnya hanya
     * diterapkan kalau user masih berada di ayat yang sama (bisa sudah pindah
     * saat pembacaan file berjalan).
     */
    private fun refreshAyahStats(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = statsStore.statsFor(surahNumber, ayahNumber)
            val s = currentReady() ?: return@launch
            if (s.surahNumber == surahNumber && s.ayah?.number == ayahNumber) {
                updateReady { it.copy(ayahStats = stats) }
            }
        }
    }

    /**
     * Simpan satu percobaan (hasil final) ke riwayat bacaan: skor, jumlah
     * percobaan, dan kata yang salah/terlewat. Hanya hasil yang benar-benar
     * ada ucapan yang dihitung (transkrip kosong = bukan percobaan baca).
     * I/O disk dijalankan di Dispatchers.IO; hasilnya hanya diterapkan kalau
     * user masih di ayat yang sama (bisa sudah pindah saat penulisan selesai).
     */
    private fun recordAttempt(text: String, words: List<String>) {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        if (text.isBlank() || words.isEmpty() || s.alignedWords.isEmpty()) return
        val surahNumber = s.surahNumber
        val ayahNumber = ayah.number
        val aligned = s.alignedWords
        viewModelScope.launch(Dispatchers.IO) {
            statsStore.record(surahNumber, ayahNumber, aligned, words)
            val updated = statsStore.statsFor(surahNumber, ayahNumber)
            // XP untuk bacaan yang baik: skor 0..100 dari proporsi kata benar.
            val score = ReadingStats.scoreOf(aligned)
            val xp = when {
                score >= 90 -> Gamification.XP_AYAH_PERFECT
                score >= 70 -> Gamification.XP_AYAH_GOOD
                else -> 0
            }
            if (xp > 0) GamificationHub.award(app, xp)
            val cur = currentReady() ?: return@launch
            if (cur.surahNumber == surahNumber && cur.ayah?.number == ayahNumber) {
                updateReady { it.copy(ayahStats = updated) }
            }
        }
    }

    private fun onTranscript(text: String, words: List<String>) {
        val aligned = TranscriptAligner.align(text, words)
        updateReady { it.copy(
            transcript = text,
            alignedWords = aligned,
            issues = issuesFor(aligned, words),
        ) }
    }

    private fun issuesFor(text: String, words: List<String>): List<ReadingIssue> =
        issuesFor(TranscriptAligner.align(text, words), words)

    private fun issuesFor(aligned: List<AlignedWord>, words: List<String>): List<ReadingIssue> = aligned
            .filter { it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED }
            .map { w ->
                ReadingIssue(
                    wordIndex = w.index,
                    word = w.referenceWord,
                    spoken = w.spokenWord,
                    rules = rulesFor(w.index, words),
                )
            }

    private fun rulesFor(index: Int, words: List<String>): List<TajwidRule> {
        if (index !in words.indices) return emptyList()
        val prev = words.getOrNull(index - 1)
        val next = words.getOrNull(index + 1)
        return TajwidEngine.analyzeWord(words[index], prev, next)
    }

    // ---- detail kata ----

    fun selectWord(index: Int) {
        val s = currentReady() ?: return
        val words = s.ayah?.words ?: return
        if (index < 0 || index !in words.indices) {
            updateReady { it.copy(
                selectedWordIndex = null,
                selectedWordRules = emptyList(),
                selectedWordMeaning = null,
                selectedWordRoot = null,
            ) }
            return
        }
        val word = words.getOrNull(index)
        updateReady { it.copy(
            selectedWordIndex = index,
            selectedWordRules = rulesFor(index, words),
            selectedWordMeaning = word?.let { w ->
                VocabularyEngine.meaningOfWord(vocabEntries, w, it.language)
            },
            selectedWordRoot = word?.let { w ->
                MorphologyEngine.lookupRoot(w)
            },
        ) }
    }

    fun toggleTajwidColor() {
        val next = !settings.tajwidColor
        settings.tajwidColor = next
        _settingsState.update { it.copy(tajwidColor = next) }
        updateReady { it.copy(tajwidColor = next) }
    }

    // ---- audio: unduh per surah (progress di footer, multi-surah) ----

    fun playAyah() {
        val s = currentReady() ?: return
        val surah = s.surah ?: return
        if (surah.ayahs.isEmpty() || s.ayah == null) return
        if (!downloader.isSurahAudioComplete(surah)) {
            updateReady { it.copy(showDownloadNotice = true) }
        }
        startSurahDownloadIfNeeded(surah) { playAyahNow() }
    }

    /**
     * Mulai unduh audio surah kalau belum lengkap (tanpa popup — progress di
     * footer), lalu jalankan `onComplete` begitu audio siap. Mendukung beberapa
     * surah yang diunduh sekaligus.
     */
    private fun startSurahDownloadIfNeeded(
        surah: Surah,
        promptBackground: Boolean = true,
        onComplete: () -> Unit,
    ) {
        // Saat bulk unduh semua berjalan, play langsung ke URL streaming.
        if (currentReady()?.isDownloadingAll == true) {
            onComplete()
            return
        }
        if (downloader.isSurahAudioComplete(surah)) {
            onComplete()
            return
        }
        if (promptBackground) maybePromptBackground()
        if (activeDownloads.containsKey(surah.number)) {
            pendingCallbacks.getOrPut(surah.number) { mutableListOf() } += onComplete
            return
        }
        val total = surah.ayahs.size + surah.ayahs.sumOf { it.words.size }
        activeTotals[surah.number] = total
        activeDone[surah.number] = 0
        // Status global untuk manajer audio.
        DownloadProgress.update { it.copy(
            isDownloading = true,
            currentSurahNumber = surah.number,
            currentSurahName = surah.nameLatin,
            surahDone = 0,
            surahTotal = total,
        ) }
        updateDownloadState()
        activeDownloads[surah.number] = viewModelScope.launch {
            try {
                downloader.downloadSurah(surah) { done, _ ->
                    activeDone[surah.number] = done
                    DownloadProgress.update { it.copy(surahDone = done) }
                    updateDownloadState()
                }
            } catch (e: Exception) {
                updateReady { it.copy(
                    message = "${AppStrings.of(currentLanguage()).msgDownloadFailed} ${surah.nameLatin}: ${e.message ?: AppStrings.of(currentLanguage()).msgCheckConnection}",
                ) }
            }
            activeDownloads.remove(surah.number)
            activeTotals.remove(surah.number)
            activeDone.remove(surah.number)
            if (activeDownloads.isEmpty()) DownloadProgress.reset()
            updateDownloadState()
            pendingCallbacks.remove(surah.number)?.forEach { it() }
        }
    }

    /**
     * Pulihkan unduhan yang tertinggal setelah crash/kill. File final yang sudah
     * lengkap hanya membersihkan manifest; file `.part` dilanjutkan oleh
     * [AudioDownloader] melalui HTTP Range bila server mendukungnya.
     */
    private fun resumePendingDownloads() {
        val slug = settings.reciter.slug
        val pending = downloader.pendingDownloads()
            .filter { it.reciterSlug == slug }
            .distinctBy { it.surahNumber }
        if (pending.isEmpty()) return
        viewModelScope.launch {
            pending.forEach { item ->
                if (activeDownloads.containsKey(item.surahNumber)) return@forEach
                val surah = withContext(Dispatchers.IO) {
                    repository.cachedSurahPlain(item.surahNumber)
                        ?: runCatching { repository.fetchSurah(item.surahNumber, currentLanguage()) }.getOrNull()
                } ?: return@forEach
                if (downloader.isSurahAudioComplete(surah)) {
                    downloader.clearPendingDownload(item.surahNumber, item.reciterSlug)
                } else {
                    startSurahDownloadIfNeeded(surah, onComplete = {}, promptBackground = false)
                }
            }
        }
    }

    /** Perbarui state progress agregat dari semua unduhan aktif. */
    private fun updateDownloadState() {
        val done = activeDone.values.sum()
        val total = activeTotals.values.sum()
        val downloading = activeDownloads.isNotEmpty()
        updateReady { it.copy(
            isDownloading = downloading,
            downloadDone = done,
            downloadTotal = total,
        ) }
        // Sinkronkan foreground service (life-keeping) kalau diizinkan user.
        if (settings.backgroundDownloadAllowed == true) {
            if (downloading) {
                if (!DownloadService.isRunning()) runCatching { DownloadService.start(app) }
                DownloadService.updateProgress(done, total)
            } else if (DownloadService.isRunning()) {
                runCatching { DownloadService.stop(app) }
            }
        }
    }

    // ---- unduhan latar belakang (foreground service) ----

    /** Tanya dulu (sekali) atau langsung nyalakan service sesuai keputusan tersimpan. */
    private fun maybePromptBackground() {
        when (settings.backgroundDownloadAllowed) {
            null -> updateReady { it.copy(showBackgroundPrompt = true) }
            true -> ensureBackgroundService()
            false -> Unit
        }
    }

    private fun ensureBackgroundService() {
        if (DownloadService.isRunning()) return
        runCatching { DownloadService.start(app) }
    }

    /** Jawaban prompt izin unduhan latar belakang. */
    fun setBackgroundDownloadAllowed(allowed: Boolean) {
        settings.backgroundDownloadAllowed = allowed
        updateReady { it.copy(showBackgroundPrompt = false) }
        if (allowed) ensureBackgroundService()
    }

    private fun playAyahNow() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        // Mulai putar ayat = batalkan status pemutaran kata yang tertunda.
        updateReady { it.copy(isWordPlaying = false) }
        val gen = playGeneration
        viewModelScope.launch {
            val file = runCatching { downloader.ensureAyah(s.surahNumber, ayah.number) }.getOrNull()
            // User menekan Stop saat menunggu unduhan → jangan mulai putar.
            if (gen != playGeneration) return@launch
            if (file == null && downloader.isAyahMissing(s.surahNumber, ayah.number)) {
                // Audio ayat memang tidak tersedia — langsung TTS tanpa coba URL.
                audioPlayer.speak(ayah.text)
                return@launch
            }
            audioPlayer.playAyah(s.surahNumber, ayah.number, ayah.text) {
                audioPlayer.speak(ayah.text)
                if (!audioPlayer.isArabicTtsAvailable()) {
                    showMessage(AppStrings.of(currentLanguage()).msgAudioUnavailable)
                }
            }
        }
    }

    fun playSelectedWord() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        val idx = s.selectedWordIndex ?: return
        if (ayah.words.getOrNull(idx) == null) return
        val surah = s.surah ?: return
        if (!downloader.isSurahAudioComplete(surah)) {
            updateReady { it.copy(showDownloadNotice = true) }
        }
        startSurahDownloadIfNeeded(surah) { playWordNow(idx) }
    }

    private fun playWordNow(wordIndex: Int) {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        val word = ayah.words.getOrNull(wordIndex) ?: return
        updateReady { it.copy(isWordPlaying = true) }
        viewModelScope.launch {
            val file = runCatching { downloader.ensureWord(s.surahNumber, ayah.number, wordIndex) }.getOrNull()
            if (file == null && downloader.isWordMissing(s.surahNumber, ayah.number, wordIndex)) {
                // Audio kata memang tidak tersedia — langsung TTS tanpa coba URL.
                updateReady { it.copy(isWordPlaying = false) }
                audioPlayer.speak(word)
                return@launch
            }
            audioPlayer.playWord(s.surahNumber, ayah.number, wordIndex, word) {
                // Fallback (TTS) — tidak ada status pemutaran media.
                updateReady { it.copy(isWordPlaying = false) }
                audioPlayer.speak(word)
                if (!audioPlayer.isArabicTtsAvailable()) {
                    showMessage(AppStrings.of(currentLanguage()).msgWordUnavailable)
                }
            }
        }
    }

    /** Tombol Stop di tooltip kata: hentikan pemutaran kata. */
    fun stopWordPlayback() {
        audioPlayer.stop()
    }

    /** Tombol Dengar/Stop: berhenti kalau sedang memutar; kalau tidak, putar ayat. */
    fun toggleAudioPlayback() {
        val s = currentReady() ?: return
        if (s.isAudioPlaying) {
            playGeneration++ // batalkan putar tertunda (sedang unduh)
            audioPlayer.stop()
        } else {
            playAyah()
        }
    }

    /** Tutup popup keterangan unduhan. */
    fun dismissDownloadNotice() {
        updateReady { it.copy(showDownloadNotice = false) }
    }

    /** Unduh audio SEMUA surah (isi surah dimuat dari cache/equran.id). */
    fun downloadAllAudio() {
        val s = currentReady() ?: return
        if (s.isDownloading) return
        maybePromptBackground()
        viewModelScope.launch {
            var done = 0
            var total = 0
            var failed = 0
            updateReady { it.copy(
                isDownloading = true,
                isDownloadingAll = true,
                downloadDone = 0,
                downloadTotal = 0,
                message = null,
            ) }
            val surahs = s.surahs.mapNotNull { meta ->
                repository.cachedSurahPlain(meta.number)
                    ?: runCatching { repository.fetchSurah(meta.number, currentLanguage()) }.getOrNull()
            }
            if (settings.backgroundDownloadAllowed == true) ensureBackgroundService()
            surahs.forEach { surah ->
                runCatching {
                    if (!downloader.isSurahAudioComplete(surah)) {
                        total += surah.ayahs.size + surah.ayahs.sumOf { it.words.size }
                        updateReady { it.copy(downloadTotal = total) }
                        // Status untuk manajer audio: surah mana yang sedang diunduh.
                        DownloadProgress.update { it.copy(
                            isDownloading = true,
                            currentSurahNumber = surah.number,
                            currentSurahName = surah.nameLatin,
                            surahDone = 0,
                            surahTotal = surah.ayahs.size + surah.ayahs.sumOf { it.words.size },
                        ) }
                        downloader.downloadSurah(surah) { d, t ->
                            done++
                            updateReady { it.copy(downloadDone = done) }
                            DownloadProgress.update { it.copy(surahDone = d, surahTotal = t) }
                            if (settings.backgroundDownloadAllowed == true) {
                                DownloadService.updateProgress(done, total)
                            }
                        }
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    failed++
                }
            }
            DownloadProgress.reset()
            updateReady { it.copy(
                isDownloading = false,
                isDownloadingAll = false,
                message = if (failed > 0) {
                    AppStrings.of(currentLanguage()).msgDownloadAllPartial.format(failed)
                } else {
                    AppStrings.of(currentLanguage()).msgDownloadAllDone
                },
            ) }
            if (settings.backgroundDownloadAllowed == true && DownloadService.isRunning()) {
                runCatching { DownloadService.stop(app) }
            }
        }
    }

    // ---- preferensi font & tema ----

    fun toggleDarkMode() {
        val next = !settings.darkMode
        AyahColors.isDark = next
        settings.darkMode = next
        _settingsState.update { it.copy(darkMode = next) }
        updateReady { it.copy(darkMode = next) }
    }

    // ---- pesan & helper ----

    fun showMessage(msg: String) {
        updateReady { it.copy(message = msg) }
    }

    fun clearMessage() {
        updateReady { it.copy(message = null) }
    }

    // ---- bahasa ----

    /** Bahasa aktif dari settings (fallback Indonesia). */
    private fun currentLanguage(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.code == settings.languageCode } ?: AppLanguage.ID

    /** Ganti bahasa aplikasi & terjemahan; muat ulang isi surah aktif kalau sudah siap. */
    fun setLanguage(lang: AppLanguage) {
        if (lang == currentLanguage()) return
        settings.languageCode = lang.code
        _settingsState.update { it.copy(language = lang) }
        val s = currentReady() ?: return
        updateReady { it.copy(
            language = lang,
            loadingSurah = true,
            message = null,
        ) }
        // Reload paksa: konten surah yang sudah dimuat masih berbahasa lama —
        // tanpa ini terjemahan mushaf tidak ikut ganti bahasa.
        ensurePageLoaded(s.pageIndex, force = true)
        // Arti kata terpilih ikut ganti bahasa.
        val sel = s.selectedWordIndex
        if (sel != null) selectWord(sel)
    }

    private fun currentReady(): TahsinUiState.Ready? = _uiState.value as? TahsinUiState.Ready

    /** Update state hanya jika sudah Ready — aman dipanggil dari dalam lambda apa pun. */
    private fun updateReady(transform: (TahsinUiState.Ready) -> TahsinUiState.Ready) {
        val current = _uiState.value as? TahsinUiState.Ready ?: return
        _uiState.value = transform(current)
        syncSettings()
    }

    /** Sinkronkan [settingsState] dari state mushaf terbaru. */
    private fun syncSettings() {
        val s = currentReady() ?: return
        _settingsState.value = SettingsUiState(
            language = s.language,
            darkMode = s.darkMode,
            tajwidColor = s.tajwidColor,
            showTranslation = s.showTranslation,
            reciter = s.reciter,
            audioSpeed = s.audioSpeed,
            ayahOfDayEnabled = s.ayahOfDayEnabled,
            streakReminderEnabled = s.streakReminderEnabled,
            isDownloading = s.isDownloading,
            downloadDone = s.downloadDone,
            downloadTotal = s.downloadTotal,
            showDownloadNotice = s.showDownloadNotice,
            showBackgroundPrompt = s.showBackgroundPrompt,
        )
    }

    override fun onCleared() {
        flowRestartJob?.cancel()
        GamificationEvents.endSuppression()
        speech.destroy()
        audioPlayer.release()
        toneGen?.release()
        toneGen = null
        super.onCleared()
    }
}
