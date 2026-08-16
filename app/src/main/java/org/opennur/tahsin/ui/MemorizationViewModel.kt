package org.opennur.tahsin.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opennur.tahsin.data.learning.MemorizationCard
import org.opennur.tahsin.data.learning.MemorizationEngine
import org.opennur.tahsin.data.learning.MemorizationTarget
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.quran.Surah
import org.opennur.tahsin.stt.AlignedWord
import org.opennur.tahsin.stt.ArabicSpeechRecognizer
import org.opennur.tahsin.stt.TranscriptAligner
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.MemorizationSnapshot
import org.opennur.tahsin.util.MemorizationStore
import org.opennur.tahsin.util.SettingsStore

data class MemorizationUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val card: MemorizationCard? = null,
    val ayah: Ayah? = null,
    val revealed: Boolean = false,
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val error: Boolean = false,
    // STT state
    val listening: Boolean = false,
    val transcript: String = "",
    val alignedWords: List<AlignedWord> = emptyList(),
    val sttScore: Int? = null,
    // Target selection
    val targetMode: String = "surah",  // "surah" or "juz"
    val selectedSurah: Int = 1,
    val selectedJuz: Int = 1,
    val availableSurahs: List<Surah> = emptyList(),
    val hasMicPermission: Boolean = false,
)

/** Offline-first Hifz/Murajaah queue over the bundled Quran repository. */
@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val app: Context,
    private val repository: QuranRepository,
    private val store: MemorizationStore,
    private val settings: SettingsStore,
    private val speech: ArabicSpeechRecognizer,
) : ViewModel() {

    private val _state = MutableStateFlow(MemorizationUiState())
    val state: StateFlow<MemorizationUiState> = _state.asStateFlow()

    private var surahs: List<Surah> = emptyList()
    private var pagination: MushafPagination = MushafPagination(0, 0, emptyList(), emptyList())

    init {
        refresh()
    }

    fun refreshLanguage() = refresh()

    fun reveal() {
        if (_state.value.card != null) _state.value = _state.value.copy(revealed = true)
    }

    fun remember() = answer(remembered = true)

    fun needReview() = answer(remembered = false)

    private fun answer(remembered: Boolean) {
        val card = _state.value.card ?: return
        val day = LocalDate.now().toEpochDay()
        val updated = if (remembered) {
            MemorizationEngine.remember(card, day)
        } else {
            MemorizationEngine.needReview(card, day)
        }
        viewModelScope.launch(Dispatchers.IO) {
            store.upsert(updated)
            if (remembered) {
                GamificationHub.award(app, Gamification.XP_MEMORIZATION_REVIEW)
            }
            load()
        }
    }

    // ---- Target selection ----

    fun setTargetMode(mode: String) {
        _state.value = _state.value.copy(targetMode = mode)
    }

    fun selectSurah(number: Int) {
        _state.value = _state.value.copy(selectedSurah = number)
    }

    fun selectJuz(number: Int) {
        _state.value = _state.value.copy(selectedJuz = number)
    }

    fun applyTarget() {
        val s = _state.value
        val target = if (s.targetMode == "juz") {
            MemorizationTarget.Juz(s.selectedJuz)
        } else {
            MemorizationTarget.Surah(s.selectedSurah)
        }
        viewModelScope.launch(Dispatchers.IO) {
            seedCards(target)
            load()
        }
    }

    private suspend fun seedCards(target: MemorizationTarget) {
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val cards = when (target) {
            is MemorizationTarget.Surah -> {
                val surah = repository.fetchSurah(target.number, language)
                MemorizationEngine.startingCards(target.number, surah.ayahCount)
            }
            is MemorizationTarget.Juz -> {
                val juzRefs = pagination.juzStarts.map {
                    MemorizationEngine.JuzStartRef(it.juz, it.surah, it.ayah)
                }
                val ayahCounts = surahs.associate { it.number to it.ayahCount }
                MemorizationEngine.startingCardsForJuz(target.number, juzRefs, ayahCounts)
            }
        }
        store.write(MemorizationSnapshot(cards))
    }

    // ---- STT: recite from memory ----

    fun toggleMic() {
        val s = _state.value
        if (s.listening) {
            speech.stop()
            _state.value = s.copy(listening = false)
            scoreStt()
        } else {
            val words = s.ayah?.words ?: return
            if (words.isEmpty()) return
            startListeningSession(words)
        }
    }

    private fun startListeningSession(words: List<String>) {
        _state.value = _state.value.copy(
            listening = true,
            transcript = "",
            alignedWords = emptyList(),
            sttScore = null,
        )
        speech.start(object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) {
                val aligned = TranscriptAligner.align(text, words)
                _state.value = _state.value.copy(
                    transcript = text,
                    alignedWords = aligned,
                )
            }
            override fun onResult(text: String) {
                val aligned = TranscriptAligner.align(text, words)
                _state.value = _state.value.copy(
                    transcript = text,
                    alignedWords = aligned,
                    listening = false,
                )
                scoreStt()
            }
            override fun onError(error: Int) {
                _state.value = _state.value.copy(listening = false)
            }
            override fun onListeningChanged(listening: Boolean) {
                _state.value = _state.value.copy(listening = listening)
            }
        })
    }

    private fun scoreStt() {
        val aligned = _state.value.alignedWords
        if (aligned.isEmpty()) return
        val correct = aligned.count { it.status == org.opennur.tahsin.stt.WordStatus.CORRECT }
        val score = (correct * 100 / aligned.size.coerceAtLeast(1))
        _state.value = _state.value.copy(sttScore = score)
        // Award XP if pass threshold (>= 80%)
        if (score >= 80) {
            GamificationHub.award(app, Gamification.XP_MEMORIZATION_REVIEW)
        }
    }

    fun clearSttSession() {
        _state.value = _state.value.copy(
            listening = false,
            transcript = "",
            alignedWords = emptyList(),
            sttScore = null,
        )
    }

    fun checkMicPermission() {
        val granted = ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        _state.value = _state.value.copy(hasMicPermission = granted)
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = false)
        viewModelScope.launch(Dispatchers.IO) { load() }
    }

    private suspend fun load() {
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        try {
            // Load surah metadata for target selection
            if (surahs.isEmpty()) {
                surahs = repository.surahList()
                pagination = repository.pagination()
            }
            val stored = store.read()
            val initial = if (stored.cards.isEmpty()) {
                val firstSurah = repository.fetchSurah(1, language)
                val cards = MemorizationEngine.startingCards(1, firstSurah.ayahCount)
                val snapshot = MemorizationSnapshot(cards)
                store.write(snapshot)
                snapshot
            } else {
                stored
            }
            val day = LocalDate.now().toEpochDay()
            val card = MemorizationEngine.selectNext(initial.cards, day)
            val ayah = card?.let { current ->
                repository.fetchSurah(current.surah, language).ayahs
                    .firstOrNull { it.number == current.ayah }
            }
            _state.value = MemorizationUiState(
                loading = false,
                language = language,
                card = card,
                ayah = ayah,
                revealed = false,
                dueCount = initial.cards.count { MemorizationEngine.isDue(it, day) },
                totalCount = initial.cards.size,
                availableSurahs = surahs,
                hasMicPermission = ContextCompat.checkSelfPermission(
                    app, android.Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _state.value = MemorizationUiState(
                loading = false,
                language = language,
                error = true,
            )
        }
    }
}
