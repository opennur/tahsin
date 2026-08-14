package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.tajwid.QuizQuestion
import org.opennur.tahsin.data.tajwid.TajwidQuiz
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.ArabicNormalizer
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.SearchableAyah
import org.opennur.tahsin.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** State layar kuis tajwid. */
data class TajwidQuizState(
    val loading: Boolean = true,
    val question: QuizQuestion? = null,
    /** Opsi yang dipilih user (null = belum menjawab). */
    val selected: String? = null,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    /** Label surah:ayat sumber soal. */
    val ayahLabel: String = "",
    /** Terjemahan ayat sumber (konteks belajar). */
    val translation: String = "",
    val language: AppLanguage = AppLanguage.ID,
    /** Nama surah (number → nameLatin). */
    val surahNames: Map<Int, String> = emptyMap(),
)

/**
 * Kuis tajwid: ambil ayat acak dari seluruh mushaf (indeks offline), pilih
 * satu kata ber-hukum, tanya "hukum apa pada kata ini?" dengan 4 opsi.
 */
@HiltViewModel
class TajwidQuizViewModel @Inject constructor(
    private val app: Context,
    private val repository: QuranRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TajwidQuizState())
    val state: StateFlow<TajwidQuizState> = _state.asStateFlow()

    private var index: List<SearchableAyah>? = null
    private val random = Random(System.currentTimeMillis())

    /** Ayat sumber soal aktif — untuk menerjemahkan ulang saat bahasa berganti. */
    private var currentEntry: SearchableAyah? = null

    init {
        val names = repository.surahList().associate { it.number to it.nameLatin }
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        _state.value = TajwidQuizState(surahNames = names, language = language)
        next()
    }

    /** Siapkan soal berikutnya (acak, dari seluruh mushaf). */
    fun next() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val idx = ensureIndex()
            var question: QuizQuestion? = null
            var entry: SearchableAyah? = null
            if (idx.isNotEmpty()) {
                // Coba beberapa ayat sampai dapat kata yang punya hukum.
                repeat(MAX_ATTEMPTS) {
                    if (question != null) return@repeat
                    val e = idx[random.nextInt(idx.size)]
                    val words = ArabicNormalizer.splitWords(e.arabic)
                    val q = TajwidQuiz.pickWord(words, random)
                    if (q != null) {
                        question = q
                        entry = e
                    }
                }
            }
            val prev = _state.value
            currentEntry = entry
            val label = entry?.let { e ->
                "${prev.surahNames[e.surahNumber] ?: "Surah ${e.surahNumber}"} :${e.ayahNumber}"
            }.orEmpty()
            val translation = entry?.let { e ->
                if (prev.language == AppLanguage.EN) e.translationEn else e.translationId
            }.orEmpty()
            _state.value = prev.copy(
                loading = false,
                question = question,
                selected = null,
                ayahLabel = label,
                translation = translation,
            )
        }
    }

    /**
     * Sinkronkan bahasa terbaru dari pengaturan (VM di-cache per Activity) —
     * terjemahan ayat sumber ikut bahasa baru; dipanggil UI saat layar terbuka.
     */
    fun refreshLanguage() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val s = _state.value
        if (s.language == lang) return
        _state.update { it.copy(language = lang) }
        val e = currentEntry
        if (e != null) {
            _state.update {
                it.copy(translation = if (lang == AppLanguage.EN) e.translationEn else e.translationId)
            }
        }
    }

    /** Jawab soal: skor naik kalau benar (satu jawaban per soal). */
    fun answer(option: String) {
        val s = _state.value
        val q = s.question ?: return
        if (s.selected != null) return
        val correct = TajwidQuiz.isCorrect(option, q)
        _state.update {
            it.copy(
                selected = option,
                correctCount = it.correctCount + if (correct) 1 else 0,
                totalCount = it.totalCount + 1,
            )
        }
        if (correct) {
            viewModelScope.launch(Dispatchers.IO) {
                GamificationHub.award(app, Gamification.XP_QUIZ_CORRECT)
            }
        }
    }

    private suspend fun ensureIndex(): List<SearchableAyah> {
        index?.let { return it }
        val idx = repository.searchIndex()
        index = idx
        return idx
    }

    companion object {
        private const val MAX_ATTEMPTS = 20
    }
}
