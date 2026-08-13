package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.ayatquiz.AyatQuiz
import com.tahsin.app.data.ayatquiz.AyatQuizQuestion
import com.tahsin.app.data.ayatquiz.SurahQuiz
import com.tahsin.app.data.ayatquiz.SurahQuizQuestion
import com.tahsin.app.data.quran.QuranRepository
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

/** Mode kuis ayat. */
enum class AyatQuizMode { COMPLETE, SURAH }

/** State layar "Kuis Ayat" (dua mode: Lengkapi Ayat & Tebak Surah). */
data class AyatQuizUiState(
    val loading: Boolean = true,
    val mode: AyatQuizMode = AyatQuizMode.COMPLETE,
    val completeQuestion: AyatQuizQuestion? = null,
    val surahQuestion: SurahQuizQuestion? = null,
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
 * Kuis Ayat: soal acak dari seluruh mushaf (indeks offline) dalam dua mode —
 * "Lengkapi Ayat" (kata mana yang melengkapi?) dan "Tebak Surah" (ayat ini
 * dari surah apa?). Jawaban benar memberi XP (badge ikut dievaluasi).
 */
class AyatQuizViewModel(
    private val app: Context,
    private val repository: QuranRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AyatQuizUiState())
    val state: StateFlow<AyatQuizUiState> = _state.asStateFlow()

    private var index: List<SearchableAyah>? = null
    /** Kolam kata per surah (untuk pengecoh Lengkapi Ayat). */
    private var wordsBySurah: Map<Int, List<String>>? = null
    private val random = Random(System.currentTimeMillis())

    init {
        val names = repository.surahList().associate { it.number to it.nameLatin }
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        _state.value = AyatQuizUiState(surahNames = names, language = language)
        next()
    }

    /** Ganti mode kuis (soal baru diacak lagi). */
    fun setMode(mode: AyatQuizMode) {
        if (_state.value.mode == mode) return
        _state.update { it.copy(mode = mode, selected = null) }
        next()
    }

    /** Siapkan soal berikutnya (acak, dari seluruh mushaf). */
    fun next() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val idx = ensureIndex()
            val wordsBySurah = ensureWordsBySurah()
            val mode = _state.value.mode
            var complete: AyatQuizQuestion? = null
            var surah: SurahQuizQuestion? = null
            var entry: SearchableAyah? = null
            if (idx.isNotEmpty()) {
                // Coba beberapa ayat sampai dapat soal yang valid.
                repeat(MAX_ATTEMPTS) {
                    if (complete != null || surah != null) return@repeat
                    val e = idx[random.nextInt(idx.size)]
                    when (mode) {
                        AyatQuizMode.COMPLETE -> {
                            val q = AyatQuiz.makeQuestion(
                                surahNumber = e.surahNumber,
                                ayahNumber = e.ayahNumber,
                                words = ArabicNormalizer.splitWords(e.arabic),
                                pool = wordsBySurah[e.surahNumber].orEmpty(),
                                random = random,
                            )
                            if (q != null) {
                                complete = q
                                entry = e
                            }
                        }
                        AyatQuizMode.SURAH -> {
                            val q = SurahQuiz.makeQuestion(
                                surahNumber = e.surahNumber,
                                ayahNumber = e.ayahNumber,
                                arabic = e.arabic,
                                surahNames = _state.value.surahNames.toList(),
                                random = random,
                            )
                            if (q != null) {
                                surah = q
                                entry = e
                            }
                        }
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
                completeQuestion = complete,
                surahQuestion = surah,
                selected = null,
                ayahLabel = label,
                translation = translation,
            )
        }
    }

    /** Jawab soal: skor naik kalau benar + XP (satu jawaban per soal). */
    fun answer(option: String) {
        val s = _state.value
        if (s.selected != null) return
        val correct = when (s.mode) {
            AyatQuizMode.COMPLETE -> {
                val q = s.completeQuestion ?: return
                AyatQuiz.isCorrect(option, q)
            }
            AyatQuizMode.SURAH -> {
                val q = s.surahQuestion ?: return
                SurahQuiz.isCorrect(option, q)
            }
        }
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

    private suspend fun ensureWordsBySurah(): Map<Int, List<String>> {
        wordsBySurah?.let { return it }
        val map = ensureIndex()
            .groupBy { it.surahNumber }
            .mapValues { (_, ayahs) ->
                ayahs.flatMap { ArabicNormalizer.splitWords(it.arabic) }
            }
        wordsBySurah = map
        return map
    }

    companion object {
        private const val MAX_ATTEMPTS = 20
    }
}

/** Factory manual DI (tanpa Hilt). */
fun ayatQuizViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        AyatQuizViewModel(
            app = app,
            repository = QuranRepository(app),
            settings = SettingsStore(app),
        )
    }
}
