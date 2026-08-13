package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.data.vocab.VocabQuizQuestion
import com.tahsin.app.data.vocab.VocabularyRepository
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.DreamBigProgressStore
import com.tahsin.app.util.DreamBigStats
import com.tahsin.app.util.Gamification
import com.tahsin.app.util.GamificationHub
import com.tahsin.app.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Mode layar game Dream BIG (arcade). */
enum class DreamBigMode { HOME, QUIZ, RESULT }

/** Soal yang sedang tampil + umpan balik jawaban. */
data class DreamBigQuizUi(
    val question: VocabQuizQuestion,
    val index: Int,
    val total: Int,
    val score: Int,
    val streak: Int,
    val bestStreak: Int,
    /** Jawaban yang dipilih (null = belum menjawab). */
    val selected: String? = null,
    /** true = jawaban benar; null = belum dievaluasi. */
    val correct: Boolean? = null,
)

/** State layar game. */
data class DreamBigUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val mode: DreamBigMode = DreamBigMode.HOME,
    /** Statistik arcade tersimpan (rekor & jumlah ronde). */
    val stats: DreamBigStats = DreamBigStats(),
    // Kuis
    val quiz: DreamBigQuizUi? = null,
    // Hasil ronde
    val resultScore: Int = 0,
    val resultStars: Int = 0,
    val resultBestStreak: Int = 0,
)

/**
 * Game "Dream BIG" (arcade): ronde kuis kosakata yang bisa dimainkan terus —
 * 10 soal diacak dari seluruh kosakata terkurasi setiap ronde. Tanpa level,
 * tanpa unlock; rekor skor & streak tersimpan di [DreamBigProgressStore].
 */
class DreamBigViewModel(
    private val app: Context,
    private val vocabRepository: VocabularyRepository,
    private val progressStore: DreamBigProgressStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DreamBigUiState())
    val state: StateFlow<DreamBigUiState> = _state.asStateFlow()

    private val progressMutex = Mutex()

    /** Kosakata terkurasi (kolam soal). */
    private var vocabEntries: List<VocabEntry> = emptyList()

    /** Statistik termutakhir (dibaca di init, diperbarui saat ronde selesai). */
    private var stats: DreamBigStats = DreamBigStats()

    /** Soal ronde aktif (private — UI cuma lihat state.quiz). */
    private var roundQuestions: List<VocabQuizQuestion> = emptyList()

    /** Sasaran ronde aktif (language-neutral) — untuk regenerasi saat bahasa berganti. */
    private var roundTargets: List<VocabEntry> = emptyList()

    init {
        viewModelScope.launch {
            val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID
            val loaded = withContext(Dispatchers.IO) {
                vocabRepository.curatedEntries() to progressStore.read()
            }
            vocabEntries = loaded.first
            stats = loaded.second
            _state.update {
                it.copy(loading = false, language = lang, stats = stats)
            }
        }
    }

    // ---- Ronde ----

    /** Mulai ronde baru: 10 soal acak dari seluruh kosakata terkurasi. */
    fun startRound() {
        val s = _state.value
        if (s.quiz != null) return // ronde sedang berjalan
        val pool = vocabEntries
        if (pool.size < 4) return // kolam terlalu kecil untuk pengecoh
        val random = Random.Default
        val targets = DreamBigGame.pickTargets(pool, DreamBigGame.QUESTIONS_PER_ROUND, random)
        val questions = targets.mapNotNull { target ->
            DreamBigGame.question(
                pool = pool,
                target = target,
                lang = s.language,
                reverse = random.nextBoolean(),
                random = random,
            )
        }
        if (questions.isEmpty()) return

        roundQuestions = questions
        roundTargets = targets
        _state.update {
            it.copy(
                mode = DreamBigMode.QUIZ,
                quiz = DreamBigQuizUi(
                    question = questions.first(),
                    index = 0,
                    total = questions.size,
                    score = 0,
                    streak = 0,
                    bestStreak = 0,
                ),
            )
        }
    }

    /** Jawab soal (sekali per soal). */
    fun answer(option: String) {
        val s = _state.value
        val q = s.quiz ?: return
        if (q.selected != null) return
        val correct = q.question.options[q.question.correctIndex] == option
        val score = q.score + if (correct) 1 else 0
        val streak = if (correct) q.streak + 1 else 0
        _state.update {
            it.copy(
                quiz = q.copy(
                    selected = option,
                    correct = correct,
                    score = score,
                    streak = streak,
                    bestStreak = maxOf(q.bestStreak, streak),
                ),
            )
        }
    }

    /** Soal berikutnya; soal terakhir → simpan statistik + layar hasil. */
    fun next() {
        val q = _state.value.quiz ?: return
        if (q.selected == null) return // belum menjawab / double-tap Next
        val nextIndex = q.index + 1
        if (nextIndex >= q.total) {
            finishRound(q)
        } else {
            _state.update {
                it.copy(
                    quiz = q.copy(
                        question = roundQuestions[nextIndex],
                        index = nextIndex,
                        selected = null,
                        correct = null,
                    ),
                )
            }
        }
    }

    /**
     * Ronde selesai: transisi ke layar hasil dilakukan SINKRON (sebelum IO)
     * supaya double-tap tidak menulis statistik dua kali dan tombol back
     * tidak bisa ditimpa coroutine — coroutine hanya menyimpan rekor.
     */
    private fun finishRound(q: DreamBigQuizUi) {
        _state.update {
            it.copy(
                mode = DreamBigMode.RESULT,
                quiz = null,
                resultScore = q.score,
                resultStars = DreamBigGame.stars(q.score, q.total),
                resultBestStreak = q.bestStreak,
            )
        }
        viewModelScope.launch {
            val updated = progressMutex.withLock {
                // Baca + tulis di IO: store.read() menyentuh disk.
                withContext(Dispatchers.IO) {
                    val updatedStats = progressStore.read().withRound(q.score, q.bestStreak).also { progressStore.write(it) }
                    GamificationHub.award(app, Gamification.XP_DREAM_BIG_ROUND)
                    updatedStats
                }
            }
            stats = updated
            _state.update { it.copy(stats = updated) }
        }
    }

    /** Main lagi (ronde baru). */
    fun playAgain() = startRound()

    /**
     * Sinkronkan bahasa terbaru dari pengaturan (VM di-cache per Activity) —
     * opsi soal dibuat dalam bahasa saat itu, jadi ronde aktif di-regenerasi.
     * Dipanggil UI setiap layar terbuka.
     */
    fun refreshLanguage() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val s = _state.value
        if (s.language == lang) return
        _state.update { it.copy(language = lang) }
        // Soal belum dijawab → regenerasi opsi dalam bahasa baru. Yang sudah
        // dijawab dibiarkan (skor sudah dihitung; `answer()` memblokir ulang).
        if (s.quiz != null && s.quiz.selected == null && roundTargets.isNotEmpty()) {
            regenerateRound(lang)
        }
    }

    /** Bangun ulang soal ronde aktif dalam bahasa baru (sasaran tetap sama). */
    private fun regenerateRound(lang: AppLanguage) {
        val s = _state.value
        val quiz = s.quiz ?: return
        val questions = roundTargets.mapNotNull { target ->
            DreamBigGame.question(
                pool = vocabEntries,
                target = target,
                lang = lang,
                reverse = Random.Default.nextBoolean(),
                random = Random.Default,
            )
        }
        if (questions.size != roundTargets.size) return // kolam berubah → biarkan apa adanya
        roundQuestions = questions
        _state.update {
            it.copy(quiz = quiz.copy(question = questions.getOrNull(quiz.index) ?: quiz.question))
        }
    }

    /** Kembali ke layar awal (skor terbaik tetap tersimpan). */
    fun backToHome() {
        _state.update { it.copy(mode = DreamBigMode.HOME, quiz = null) }
    }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun dreamBigViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        DreamBigViewModel(
            app = app,
            vocabRepository = VocabularyRepository(app),
            progressStore = DreamBigProgressStore.fromContext(app),
            settings = SettingsStore(app),
        )
    }
}
