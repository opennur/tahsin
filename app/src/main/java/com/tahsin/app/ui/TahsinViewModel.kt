package com.tahsin.app.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
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
import com.tahsin.app.util.AudioDownloader
import com.tahsin.app.util.FontStore
import com.tahsin.app.util.SettingsStore
import com.tahsin.app.util.TahsinAudioPlayer
import androidx.compose.ui.text.font.FontFamily
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
        val selectedWordIndex: Int? = null,
        val selectedWordRules: List<TajwidRule> = emptyList(),
        val message: String? = null,
        val fontScale: Float = 1.5f,
        val arabicFont: ArabicFont = ArabicFont.UTSMANI,
        /** FontFamily efektif (font file kalau ada, fallback sistem). */
        val arabicFontFamily: FontFamily = FontFamily.Default,
        val darkMode: Boolean = false,
        /** Pewarnaan huruf tajwid di mushaf (gaya mushaf tajwid). */
        val tajwidColor: Boolean = true,
        /** Mode flow (muroja'ah): lanjut otomatis ke ayat berikutnya saat selesai benar. */
        val flowMode: Boolean = false,
        /** Sedang memutar audio (untuk tombol Dengar/Stop). */
        val isAudioPlaying: Boolean = false,
        /** Sedang mengunduh audio (agregat semua surah, tampil di atas tombol). */
        val isDownloading: Boolean = false,
        val downloadDone: Int = 0,
        val downloadTotal: Int = 0,
        /** Bulk unduh semua surah sedang berjalan (penjaga agar tidak bertumpuk). */
        val isDownloadingAll: Boolean = false,
        /** Popup keterangan saat unduhan audio dimulai. */
        val showDownloadNotice: Boolean = false,
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

class TahsinViewModel(
    private val repository: QuranRepository,
    private val speech: ArabicSpeechRecognizer,
    private val audioPlayer: TahsinAudioPlayer,
    private val settings: SettingsStore,
    private val downloader: AudioDownloader,
    private val fontStore: FontStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TahsinUiState>(TahsinUiState.Loading)
    val uiState: StateFlow<TahsinUiState> = _uiState.asStateFlow()

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

    init {
        AyahColors.isDark = settings.darkMode
        audioPlayer.onPlaybackChange = { playing ->
            updateReady { it.copy(isAudioPlaying = playing) }
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
                arabicFont = ArabicFont.UTSMANI,
                arabicFontFamily = fontStore.loadFamily(ArabicFont.UTSMANI),
                darkMode = settings.darkMode,
                tajwidColor = settings.tajwidColor,
                flowMode = settings.flowMode,
            )
        } catch (e: Exception) {
            TahsinUiState.Error(e.message ?: "Gagal memuat mushaf.")
        }
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
    }

    // ---- navigasi surah/ayat ----

    fun selectSurah(number: Int) {
        pendingAutoListen = false // navigasi manual membatalkan auto-dengar tertunda
        settings.surahNumber = number
        settings.ayahIndex = 0
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            surahNumber = number,
            ayahIndex = 0,
            loadingSurah = true,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            message = null,
        ) }
        loadSurahContent(number)
    }

    /** Muat isi surah: cache dulu, kalau belum ada unduh dari equran.id. */
    private fun loadSurahContent(number: Int) {
        viewModelScope.launch {
            val cached = repository.cachedSurah(number)
            if (cached != null) {
                replaceSurah(cached)
                return@launch
            }
            try {
                replaceSurah(repository.fetchSurah(number))
            } catch (e: Exception) {
                updateReady { it.copy(
                    loadingSurah = false,
                    message = "Gagal memuat surah $number: ${e.message ?: "periksa koneksi"}",
                ) }
            }
        }
    }

    private fun replaceSurah(surah: Surah) {
        val s = currentReady() ?: return
        val index = s.ayahIndex.coerceIn(0, (surah.ayahs.size - 1).coerceAtLeast(0))
        if (index != s.ayahIndex) settings.ayahIndex = index
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            loadingSurah = false,
            surahs = it.surahs.map { x -> if (x.number == surah.number) surah else x },
            ayahIndex = index,
        ) }
        // Mode flow lintas surah: mulai dengar begitu konten siap.
        if (pendingAutoListen) {
            pendingAutoListen = false
            startListeningForCurrentAyah()
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
        settings.ayahIndex = index
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            ayahIndex = index,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            message = null,
        ) }
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
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
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
                maybeAutoAdvance()
                maybePlayErrorTone()
            }
            override fun onError(message: String) {
                autoAdvanceHandled = true
                _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
                    listening = false,
                    message = message,
                ) }
            }

            override fun onListeningChanged(listening: Boolean) {
                _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(listening = listening) }
            }
        })
    }

    // ---- mode flow (muroja'ah): lanjut otomatis kalau satu ayat selesai benar ----

    /** Aktifkan/nonaktifkan mode flow. */
    fun toggleFlowMode() {
        val s = currentReady() ?: return
        val next = !s.flowMode
        settings.flowMode = next
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(flowMode = next) }
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
            updateReady { it.copy(message = "✅ Ayat selesai — lanjut ke ayat berikutnya") }
            startListeningForCurrentAyah()
            return
        }
        val idx = s.surahs.indexOfFirst { it.number == surah.number }
        val nextNumber = s.surahs.getOrNull(idx + 1)?.number
        if (nextNumber != null) {
            updateReady { it.copy(message = "✅ Surah selesai — lanjut ke surah berikutnya") }
            selectSurah(nextNumber)
            pendingAutoListen = true
            return
        }
        updateReady { it.copy(message = "🎉 Selesai muroja'ah — seluruh Al-Qur'an dibaca benar!") }
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

    /** Beep gagal — ada kata yang tidak cocok / terlewat. */
    private fun playErrorTone() {
        try {
            toneGenerator()?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
        } catch (_: Exception) {
        }
    }

    /** Saat hasil final: kalau mode flow nyala dan ada yang salah, kasih beep. */
    private fun maybePlayErrorTone() {
        if (autoAdvanceHandled) return // sudah sukses — beep sukses sudah diputar
        val s = currentReady() ?: return
        if (!s.flowMode) return
        val aligned = s.alignedWords
        val hasError = aligned.any {
            it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED
        }
        if (hasError) playErrorTone()
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
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
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
            _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
                selectedWordIndex = null,
                selectedWordRules = emptyList(),
            ) }
            return
        }
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            selectedWordIndex = index,
            selectedWordRules = rulesFor(index, words),
        ) }
    }

    fun toggleTajwidColor() {
        val s = currentReady() ?: return
        val next = !s.tajwidColor
        settings.tajwidColor = next
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(tajwidColor = next) }
    }

    // ---- audio: unduh per surah (progress di footer, multi-surah) ----

    fun playAyah() {
        val s = currentReady() ?: return
        val surah = s.surah ?: return
        if (surah.ayahs.isEmpty() || s.ayah == null) return
        if (!downloader.isSurahAudioComplete(surah)) {
            _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(showDownloadNotice = true) }
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
        if (activeDownloads.containsKey(surah.number)) {
            pendingCallbacks.getOrPut(surah.number) { mutableListOf() } += onComplete
            return
        }
        val total = surah.ayahs.size + surah.ayahs.sumOf { it.words.size }
        activeTotals[surah.number] = total
        activeDone[surah.number] = 0
        updateDownloadState()
        activeDownloads[surah.number] = viewModelScope.launch {
            try {
                downloader.downloadSurah(surah) { done, _ ->
                    activeDone[surah.number] = done
                    updateDownloadState()
                }
            } catch (e: Exception) {
                updateReady { it.copy(
                    message = "Gagal mengunduh ${surah.nameLatin}: ${e.message ?: "periksa koneksi"}",
                ) }
            }
            activeDownloads.remove(surah.number)
            activeTotals.remove(surah.number)
            activeDone.remove(surah.number)
            updateDownloadState()
            pendingCallbacks.remove(surah.number)?.forEach { it() }
        }
    }

    /** Perbarui state progress agregat dari semua unduhan aktif. */
    private fun updateDownloadState() {
        val done = activeDone.values.sum()
        val total = activeTotals.values.sum()
        updateReady { it.copy(
            isDownloading = activeDownloads.isNotEmpty(),
            downloadDone = done,
            downloadTotal = total,
        ) }
    }

    private fun playAyahNow() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        viewModelScope.launch {
            runCatching { downloader.ensureAyah(s.surahNumber, ayah.number) }
            audioPlayer.playAyah(s.surahNumber, ayah.number, ayah.text) {
                audioPlayer.speak(ayah.text)
                if (!audioPlayer.isArabicTtsAvailable()) {
                    showMessage("Audio belum tersedia. Cek koneksi lalu coba lagi.")
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
            _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(showDownloadNotice = true) }
        }
        startSurahDownloadIfNeeded(surah) { playWordNow(idx) }
    }

    private fun playWordNow(wordIndex: Int) {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        val word = ayah.words.getOrNull(wordIndex) ?: return
        viewModelScope.launch {
            runCatching { downloader.ensureWord(s.surahNumber, ayah.number, wordIndex) }
            audioPlayer.playWord(s.surahNumber, ayah.number, wordIndex, word) {
                audioPlayer.speak(word)
                if (!audioPlayer.isArabicTtsAvailable()) {
                    showMessage("Audio kata belum tersedia. Cek koneksi lalu coba lagi.")
                }
            }
        }
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
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(showDownloadNotice = false) }
    }

    /**
     * Unduh audio SEMUA surah (isi surah otomatis dimuat dari cache/equran.id).
     * Berjalan sekuensial; progres agregat tampil di bar atas tombol.
     */
    fun downloadAllAudio() {
        val s = currentReady() ?: return
        if (s.isDownloading) return
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
            s.surahs.forEach { meta ->
                val surah = repository.cachedSurah(meta.number)
                    ?: runCatching { repository.fetchSurah(meta.number) }.getOrNull()
                if (surah == null) {
                    failed++
                    return@forEach
                }
                replaceSurah(surah)
                if (!downloader.isSurahAudioComplete(surah)) {
                    total += surah.ayahs.size + surah.ayahs.sumOf { it.words.size }
                    updateReady { it.copy(downloadTotal = total) }
                    try {
                        downloader.downloadSurah(surah) { _, _ ->
                            done++
                            updateReady { it.copy(downloadDone = done) }
                        }
                    } catch (e: Exception) {
                        failed++
                    }
                }
            }
            updateReady { it.copy(
                isDownloading = false,
                isDownloadingAll = false,
                message = if (failed > 0) {
                    "Unduh selesai: $failed surah gagal."
                } else {
                    "Semua audio berhasil diunduh ✓"
                },
            ) }
        }
    }

    // ---- preferensi font & tema ----

    fun toggleDarkMode() {
        val s = currentReady() ?: return
        val next = !s.darkMode
        AyahColors.isDark = next
        settings.darkMode = next
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(darkMode = next) }
    }

    // ---- pesan & helper ----

    fun showMessage(msg: String) {
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(message = msg) }
    }

    fun clearMessage() {
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(message = null) }
    }

    private fun currentReady(): TahsinUiState.Ready? = _uiState.value as? TahsinUiState.Ready

    /** Update state hanya jika sudah Ready — aman dipanggil dari dalam lambda apa pun. */
    private fun updateReady(transform: (TahsinUiState.Ready) -> TahsinUiState.Ready) {
        val current = _uiState.value as? TahsinUiState.Ready ?: return
        _uiState.value = transform(current)
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
            repository = QuranRepository(app),
            speech = ArabicSpeechRecognizer(app),
            audioPlayer = TahsinAudioPlayer(app),
            settings = SettingsStore(app),
            downloader = AudioDownloader(app),
            fontStore = FontStore(app),
        )
    }
}
