package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.tahsin.app.util.TahsinAudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** State layar utama Tahsin. */
sealed interface TahsinUiState {
    data object Loading : TahsinUiState

    data class Error(val message: String) : TahsinUiState

    data class Ready(
        val surahs: List<Surah> = emptyList(),
        val surahNumber: Int = 1,
        val ayahIndex: Int = 0,
        val listening: Boolean = false,
        val transcript: String = "",
        val alignedWords: List<AlignedWord> = emptyList(),
        val issues: List<ReadingIssue> = emptyList(),
        val selectedWordIndex: Int? = null,
        val selectedWordRules: List<TajwidRule> = emptyList(),
        val message: String? = null,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<TahsinUiState>(TahsinUiState.Loading)
    val uiState: StateFlow<TahsinUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _uiState.value = TahsinUiState.Loading
        _uiState.value = try {
            TahsinUiState.Ready(surahs = repository.surahs())
        } catch (e: Exception) {
            TahsinUiState.Error(e.message ?: "Gagal memuat mushaf.")
        }
    }

    // ---- navigasi surah/ayat ----

    fun selectSurah(number: Int) {
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            surahNumber = number,
            ayahIndex = 0,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            selectedWordIndex = null,
            selectedWordRules = emptyList(),
            message = null,
        ) }
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

    private fun updateAyah(index: Int) {
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
        val ayah = s.ayah ?: return
        val words = ayah.words
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(
            listening = true,
            transcript = "",
            alignedWords = emptyList(),
            issues = emptyList(),
            message = null,
        ) }
        speech.start(object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) = onTranscript(text, words)
            override fun onResult(text: String) = onTranscript(text, words)
            override fun onError(message: String) {
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

    fun playSelectedWord() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        val idx = s.selectedWordIndex ?: return
        val word = ayah.words.getOrNull(idx) ?: return
        audioPlayer.playWord(
            surahNumber = s.surahNumber,
            ayahNumber = ayah.number,
            wordIndex = idx,
            word = word,
        ) {
            audioPlayer.speak(word)
            if (!audioPlayer.isArabicTtsAvailable()) {
                showMessage(
                    "Audio kata belum tersedia. Untuk offline, jalankan " +
                        "tools/download_minshawi.sh lalu build ulang.",
                )
            }
        }
    }

    fun selectAyah(index: Int) {
        val s = currentReady() ?: return
        val max = (s.surah?.ayahs?.size ?: 1) - 1
        if (index in 0..max) updateAyah(index)
    }

    fun playAyah() {
        val s = currentReady() ?: return
        val ayah = s.ayah ?: return
        audioPlayer.playAyah(
            surahNumber = s.surahNumber,
            ayahNumber = ayah.number,
            audioUrl = ayah.audioUrl,
            text = ayah.text,
        ) {
            audioPlayer.speak(ayah.text)
            if (!audioPlayer.isArabicTtsAvailable()) {
                showMessage(
                    "Audio belum tersedia. Untuk mode offline, jalankan " +
                        "tools/download_minshawi.sh lalu build ulang.",
                )
            }
        }
    }

    // ---- pesan ----

    fun showMessage(msg: String) {
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(message = msg) }
    }

    fun clearMessage() {
        _uiState.update { (it as? TahsinUiState.Ready ?: return).copy(message = null) }
    }

    private fun currentReady(): TahsinUiState.Ready? = _uiState.value as? TahsinUiState.Ready

    override fun onCleared() {
        speech.destroy()
        audioPlayer.release()
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
        )
    }
}
