package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.quran.QuranRepository
import com.tahsin.app.data.tajwid.QuizQuestion
import com.tahsin.app.data.tajwid.TajwidQuiz
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.ArabicNormalizer
import com.tahsin.app.util.Gamification
import com.tahsin.app.util.GamificationHub
import com.tahsin.app.util.SearchableAyah
import com.tahsin.app.util.SettingsStore
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
class TajwidQuizViewModel(
    private val app: Context,
    private val repository: QuranRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TajwidQuizState())
    val state: StateFlow<TajwidQuizState> = _state.asStateFlow()

    private var index: List<SearchableAyah>? = null
    private val random = Random(System.currentTimeMillis())

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

/** Factory manual DI (tanpa Hilt). */
fun tajwidQuizViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        TajwidQuizViewModel(
            app = app,
            repository = QuranRepository(app),
            settings = SettingsStore(app),
        )
    }
}
