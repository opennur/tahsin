package com.tahsin.app.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.quran.Ayah
import com.tahsin.app.data.quran.QuranRepository
import com.tahsin.app.data.quran.Surah
import com.tahsin.app.data.tajwid.TajwidEngine
import com.tahsin.app.data.tajwid.TajwidRule
import com.tahsin.app.stt.ArabicSpeechRecognizer
import com.tahsin.app.stt.AlignedWord
import com.tahsin.app.stt.TranscriptAligner
import com.tahsin.app.stt.WordStatus
import com.tahsin.app.theme.ArabicFont
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.ui.AppStrings
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.AudioDownloader
import com.tahsin.app.util.DownloadProgress
import com.tahsin.app.util.DownloadService
import com.tahsin.app.util.FontStore
import com.tahsin.app.util.Gamification
import com.tahsin.app.util.GamificationHub
import com.tahsin.app.util.ReadingStats
import com.tahsin.app.util.AyahStats
import com.tahsin.app.util.PlaySource
import com.tahsin.app.util.ReadingStatsStore
import com.tahsin.app.util.Reciter
import com.tahsin.app.util.SettingsStore
import com.tahsin.app.util.TahsinAudioPlayer
import com.tahsin.app.widget.StreakReminderAlarm
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State layar utama Tahsin. */
sealed interface TahsinUiState {
    data object Loading : TahsinUiState

    data class Error(val message: String) : TahsinUiState

    data class Ready(
        val surahs: List<Surah> = emptyList(),
        val surahNumber: Int = 1,
        val ayahIndex: Int = 0,
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
        val message: String? = null,
        val fontScale: Float = 1.5f,
        val language: AppLanguage = AppLanguage.ID,
        val arabicFont: ArabicFont = ArabicFont.UTSMANI,
        /** FontFamily efektif (font file kalau ada, fallback sistem). */
        val arabicFontFamily: FontFamily = FontFamily.Default,
        val darkMode: Boolean = false,
        /** Pewarnaan huruf tajwid di mushaf (gaya mushaf tajwid). */
        val tajwidColor: Boolean = true,
        /** Mode flow (muroja'ah): lanjut otomatis ke ayat berikutnya saat selesai benar. */
        val flowMode: Boolean = false,
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
    val flowMode: Boolean = false,
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

class TahsinViewModel(
    private val app: Context,
    private val repository: QuranRepository,
    private val speech: ArabicSpeechRecognizer,
    private val audioPlayer: TahsinAudioPlayer,
    private val settings: SettingsStore,
    private val downloader: AudioDownloader,
    private val fontStore: FontStore,
    private val statsStore: ReadingStatsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TahsinUiState>(TahsinUiState.Loading)
    val uiState: StateFlow<TahsinUiState> = _uiState.asStateFlow()

    /** State setelan lintas layar (portal Home & Pengaturan) — diinisialisasi
     * dari penyimpanan, lalu disinkronkan via [syncSettings] tiap state berubah. */
    private val _settingsState = MutableStateFlow(
        SettingsUiState(
            language = currentLanguage(),
            darkMode = settings.darkMode,
            tajwidColor = settings.tajwidColor,
            flowMode = settings.flowMode,
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

    /** Cegah auto-advance ganda dalam satu sesi baca (di-reset tiap mulai dengar). */
    private var autoAdvanceHandled = false
    /** Auto-dengar tertunda sampai konten surah termuat (lintas surah). */
    private var pendingAutoListen = false
    /** Target buka dari widget/notifikasi "Ayah of the Day" (surah, ayat 1-based). */
    private var pendingOpenAt: Pair<Int, Int>? = null
    /** Ayat 0-based yang harus dipilih setelah konten surah termuat. */
    private var pendingAyahAfterLoad: Int? = null

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
        reload()
    }

    fun reload() {
        _uiState.value = TahsinUiState.Loading
        _uiState.value = try {
            TahsinUiState.Ready(
                surahs = repository.surahList(),
                surahNumber = settings.surahNumber,
                ayahIndex = settings.ayahIndex,
                loadingSurah = true,
                fontScale = 1.5f,
                language = currentLanguage(),
                arabicFont = ArabicFont.UTSMANI,
                arabicFontFamily = fontStore.loadFamily(ArabicFont.UTSMANI),
                darkMode = settings.darkMode,
                tajwidColor = settings.tajwidColor,
                flowMode = settings.flowMode,
                reciter = settings.reciter,
                audioSpeed = settings.audioSpeed,
                ayahOfDayEnabled = settings.ayahOfDayEnabled,
                streakReminderEnabled = settings.streakReminderEnabled,
                showSwipeHint = !settings.swipeHintDismissed,
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
        // Muat isi surah terakhir yang dibuka (default: Al-Fatihah ayat 1).
        (currentReady())?.let { loadSurahContent(it.surahNumber) }
        // Target dari widget/notifikasi (state baru saja siap) → buka langsung.
        pendingOpenAt?.let { (s, a) -> openAt(s, a) }
    }

    /** Tutup petunjuk geser (persisten — tidak muncul lagi setelah restart). */
    fun dismissSwipeHint() {
        settings.swipeHintDismissed = true
        updateReady { it.copy(showSwipeHint = false) }
    }

    // ---- navigasi surah/ayat ----

    fun selectSurah(number: Int) {
        pendingAutoListen = false // navigasi manual membatalkan auto-dengar tertunda
        // Navigasi manual membatalkan target widget/notifikasi yang belum terpakai
        // (mis. konten surah target belum termuat saat user pindah surah lain).
        pendingOpenAt = null
        pendingAyahAfterLoad = null
        navigateToSurah(number)
    }

    private fun navigateToSurah(number: Int) {
        settings.surahNumber = number
        settings.ayahIndex = 0
        updateReady { it.copy(
            surahNumber = number,
            ayahIndex = 0,
            loadingSurah = true,
            ayahStats = null,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            message = null,
        ) }
        refreshAyahStats(number, 1)
        loadSurahContent(number)
    }

    /** Muat isi surah: cache dulu, kalau belum ada unduh dari equran.id. */
    private fun loadSurahContent(number: Int) {
        viewModelScope.launch {
            val lang = currentLanguage()
            val cached = repository.cachedSurah(number, lang)
            if (cached != null) {
                replaceSurah(cached)
                return@launch
            }
            try {
                replaceSurah(repository.fetchSurah(number, lang))
            } catch (e: Exception) {
                updateReady { it.copy(
                    loadingSurah = false,
                    message = "${AppStrings.of(lang).msgSurahLoadFailed} $number: ${e.message ?: AppStrings.of(lang).msgCheckConnection}",
                ) }
            }
        }
    }

    private fun replaceSurah(surah: Surah) {
        val s = currentReady() ?: return
        val index = s.ayahIndex.coerceIn(0, (surah.ayahs.size - 1).coerceAtLeast(0))
        if (index != s.ayahIndex) settings.ayahIndex = index
        val ayah = surah.ayahs.getOrNull(index)
        updateReady { it.copy(
            loadingSurah = false,
            surahs = it.surahs.map { x -> if (x.number == surah.number) surah else x },
            ayahIndex = index,
            ayahStats = null,
        ) }
        refreshAyahStats(surah.number, ayah?.number ?: 1)
        // Mode flow lintas surah: mulai dengar begitu konten siap.
        if (pendingAutoListen) {
            pendingAutoListen = false
            startListeningForCurrentAyah()
        }
        // Target dari widget/notifikasi: pilih ayat setelah konten surah termuat.
        pendingAyahAfterLoad?.let { idx ->
            pendingAyahAfterLoad = null
            updateAyah(idx.coerceIn(0, surah.ayahs.lastIndex.coerceAtLeast(0)))
        }
    }

    fun nextAyah() {
        val s = currentReady() ?: return
        val max = (s.surah?.ayahs?.size ?: 1) - 1
        if (s.ayahIndex < max) updateAyah(s.ayahIndex + 1)
    }

    fun prevAyah() {
        val s = currentReady() ?: return
        if (s.ayahIndex > 0) updateAyah(s.ayahIndex - 1)
    }

    fun selectAyah(index: Int) {
        val s = currentReady() ?: return
        val max = (s.surah?.ayahs?.size ?: 1) - 1
        if (index in 0..max) updateAyah(index)
    }

    private fun updateAyah(index: Int) {
        pendingAyahAfterLoad = null // navigasi apa pun membatalkan target tertunda
        settings.ayahIndex = index
        val s = currentReady() ?: return
        val ayahNumber = index + 1 // nomor ayat 1-based
        updateReady { it.copy(
            ayahIndex = index,
            ayahStats = null,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            message = null,
        ) }
        refreshAyahStats(s.surahNumber, ayahNumber)
    }

    // ---- buka target (widget "Ayah of the Day" / notifikasi) ----

    /** Buka surah/ayat tertentu; aman dipanggil kapan saja (state bisa belum siap). */
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
        val idx = (ayahNumber - 1).coerceAtLeast(0)
        if (s.surahNumber == surahNumber && (s.surah?.ayahs?.size ?: 0) > 0) {
            updateAyah(idx.coerceAtMost(s.surah!!.ayahs.lastIndex))
        } else {
            pendingAyahAfterLoad = idx
            navigateToSurah(surahNumber)
        }
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
        if (s.listening) {
            speech.stop()
            return
        }
        pendingAutoListen = false // inisiatif manual membatalkan auto-dengar tertunda
        val words = s.ayah?.words.orEmpty()
        if (words.isEmpty()) return
        startListeningSession(words)
    }

    private fun startListeningSession(words: List<String>) {
        autoAdvanceHandled = false
        updateReady { it.copy(
            listening = true,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            message = null,
        ) }
        speech.start(object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) = onTranscript(text, words)
            override fun onResult(text: String) {
                onTranscript(text, words)
                recordAttempt(text, words)
                maybeAutoAdvance()
                maybePlayErrorTone()
            }
            override fun onError(error: Int) {
                autoAdvanceHandled = true
                updateReady { it.copy(
                    listening = false,
                    message = AppStrings.sttErrorMessage(error, currentLanguage()),
                ) }
            }

            override fun onListeningChanged(listening: Boolean) {
                updateReady { it.copy(listening = listening) }
            }
        })
    }

    // ---- mode flow (muroja'ah): lanjut otomatis kalau satu ayat selesai benar ----

    /** Aktifkan/nonaktifkan mode flow. Persisten walau Tahsin belum siap. */
    fun toggleFlowMode() {
        val next = !settings.flowMode
        settings.flowMode = next
        _settingsState.update { it.copy(flowMode = next) }
        updateReady { it.copy(flowMode = next) }
    }

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
     * kata terbaca benar (tidak ada yang terlewat/salah), jadwalkan pindah ke
     * ayat berikutnya lalu lanjut mendengar (muroja'ah berkelanjutan).
     */
    private fun maybeAutoAdvance() {
        if (autoAdvanceHandled) return
        val s = currentReady() ?: return
        if (!s.flowMode) return
        val words = s.ayah?.words.orEmpty()
        val aligned = s.alignedWords
        if (words.isEmpty() || aligned.isEmpty()) return
        val perfect = aligned.size == words.size && aligned.all { it.status == WordStatus.CORRECT }
        if (!perfect) return
        autoAdvanceHandled = true
        playSuccessTone()
        val fromSurah = s.surahNumber
        val fromAyah = s.ayahIndex
        viewModelScope.launch {
            delay(1200)
            val st = currentReady() ?: return@launch
            if (!st.flowMode) return@launch
            if (st.listening) return@launch          // user sudah mulai baca lagi — jangan ganggu
            if (st.surahNumber != fromSurah || st.ayahIndex != fromAyah) return@launch
            advanceToNext()
        }
    }

    private fun advanceToNext() {
        val s = currentReady() ?: return
        val surah = s.surah ?: return
        if (s.ayahIndex < surah.ayahs.size - 1) {
            updateAyah(s.ayahIndex + 1)
            updateReady { it.copy(message = AppStrings.of(currentLanguage()).msgAyahDone) }
            startListeningForCurrentAyah()
            return
        }
        val idx = s.surahs.indexOfFirst { it.number == surah.number }
        val nextNumber = s.surahs.getOrNull(idx + 1)?.number
        if (nextNumber != null) {
            updateReady { it.copy(message = AppStrings.of(currentLanguage()).msgSurahDone) }
            selectSurah(nextNumber)
            pendingAutoListen = true
            return
        }
        updateReady { it.copy(message = AppStrings.of(currentLanguage()).msgMurojaahDone) }
    }

    /** Mulai mendengar ayat yang sedang aktif; tunda dulu kalau konten belum siap. */
    private fun startListeningForCurrentAyah() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        if (s.loadingSurah || ayah.words.isEmpty()) {
            pendingAutoListen = true
            return
        }
        startListeningSession(ayah.words)
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

    /** Saat hasil final: kalau ada kata yang salah, kasih umpan gagal (bunyi + getar). */
    private fun maybePlayErrorTone() {
        if (autoAdvanceHandled) return // sudah sukses — beep sukses sudah diputar
        val s = currentReady() ?: return
        val aligned = s.alignedWords
        val hasError = aligned.any {
            it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED
        }
        if (hasError) playErrorFeedback()
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
        val issues = aligned
            .filter { it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED }
            .map { w ->
                ReadingIssue(
                    wordIndex = w.index,
                    word = w.referenceWord,
                    spoken = w.spokenWord,
                    rules = rulesFor(w.index, words),
                )
            }
        updateReady { it.copy(
            transcript = text,
            alignedWords = aligned,
            issues = issues,
        ) }
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
            ) }
            return
        }
        updateReady { it.copy(
            selectedWordIndex = index,
            selectedWordRules = rulesFor(index, words),
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
    private fun startSurahDownloadIfNeeded(surah: Surah, onComplete: () -> Unit) {
        // Saat bulk unduh semua berjalan, play langsung ke URL streaming.
        if (currentReady()?.isDownloadingAll == true) {
            onComplete()
            return
        }
        if (downloader.isSurahAudioComplete(surah)) {
            onComplete()
            return
        }
        maybePromptBackground()
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
        viewModelScope.launch {
            val file = runCatching { downloader.ensureAyah(s.surahNumber, ayah.number) }.getOrNull()
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
                }.onFailure { failed++ }
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
        loadSurahContent(s.surahNumber)
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
            flowMode = s.flowMode,
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
        speech.destroy()
        audioPlayer.release()
        toneGen?.release()
        toneGen = null
        super.onCleared()
    }
}

/** Factory manual DI (tanpa Hilt). */
fun tahsinViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        TahsinViewModel(
            app = app,
            repository = QuranRepository(app),
            speech = ArabicSpeechRecognizer(app),
            audioPlayer = TahsinAudioPlayer(app, SettingsStore(app)),
            settings = SettingsStore(app),
            downloader = AudioDownloader(app, SettingsStore(app)),
            fontStore = FontStore(app),
            statsStore = ReadingStatsStore(app),
        )
    }
}
