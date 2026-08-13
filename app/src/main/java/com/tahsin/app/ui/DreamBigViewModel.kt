package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.data.dreambig.DreamBigLevel
import com.tahsin.app.data.dreambig.DreamBigParser
import com.tahsin.app.data.dreambig.DreamBigRepository
import com.tahsin.app.data.dreambig.DreamBigVideo
import com.tahsin.app.data.dreambig.TranscriptParagraph
import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.data.vocab.VocabQuizQuestion
import com.tahsin.app.data.vocab.VocabularyRepository
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.DreamBigProgress
import com.tahsin.app.util.DreamBigProgressStore
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

/** Mode layar game Dream BIG. */
enum class DreamBigMode { LEVELS, QUIZ, RESULT, TRANSCRIPT }

/** Satu kartu level di peta (state tampilan, bukan data mentah). */
data class DreamBigLevelUi(
    val day: Int,
    val title: String,
    val locked: Boolean,
    val bestScore: Int,
    val stars: Int,
)

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
    val mode: DreamBigMode = DreamBigMode.LEVELS,
    // Peta level
    val levels: List<DreamBigLevelUi> = emptyList(),
    // Kuis
    val quiz: DreamBigQuizUi? = null,
    // Hasil ronde
    val resultDay: Int = 0,
    val resultScore: Int = 0,
    val resultStars: Int = 0,
    val resultBestStreak: Int = 0,
    val resultPassed: Boolean = false,
    // Materi (transkrip)
    val transcriptVideos: List<DreamBigVideo> = emptyList(),
    val transcriptVideo: DreamBigVideo? = null,
    val transcriptParagraphs: List<TranscriptParagraph> = emptyList(),
)

/**
 * Game "Dream BIG": 10 level kuis kosakata Qur'an (Day 1..10). Soal diambil
 * dari kosakata terkurasi existing; transkrip tiap hari = materi pendukung.
 */
class DreamBigViewModel(
    private val repository: DreamBigRepository,
    private val vocabRepository: VocabularyRepository,
    private val progressStore: DreamBigProgressStore,
    settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DreamBigUiState())
    val state: StateFlow<DreamBigUiState> = _state.asStateFlow()

    private val progressMutex = Mutex()

    // ---- Data muatan init (baca sekali, dipakai level & kuis) ----
    private var levelData: List<DreamBigLevel> = emptyList()
    private var vocabEntries: List<VocabEntry> = emptyList()

    /** Progres termutakhir (dibaca di init, diperbarui saat ronde selesai). */
    private var progress: DreamBigProgress = DreamBigProgress()

    /** Soal ronde level aktif (private — UI cuma lihat state.quiz). */
    private var roundQuestions: List<VocabQuizQuestion> = emptyList()

    /** Level aktif (day). */
    private var activeDay = 0

    /** Naik tiap pemuatan transkrip; hasil IO basi dibuang lewat perbandingan id. */
    private var transcriptRequestId = 0L

    init {
        viewModelScope.launch {
            val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID
            val loaded = withContext(Dispatchers.IO) {
                val levels = repository.levels()
                val vocab = vocabRepository.curatedEntries()
                val prog = progressStore.read()
                Triple(levels, vocab, prog)
            }
            levelData = loaded.first
            vocabEntries = loaded.second
            progress = loaded.third
            _state.update {
                it.copy(
                    loading = false,
                    language = lang,
                    levels = toLevelUi(levelData, progress),
                )
            }
        }
    }

    // ---- Peta level ----

    /** Buka level [day]: siapkan ronde kuis 10 soal dari kolam kata level itu. */
    fun startLevel(day: Int) {
        val s = _state.value
        val level = levelData.firstOrNull { it.day == day } ?: return
        val levelUi = s.levels.firstOrNull { it.day == day } ?: return
        if (levelUi.locked) return

        val pool = DreamBigGame.wordsFor(level, vocabEntries)
        if (pool.size < 4) return // kolam terlalu kecil untuk pengecoh
        val random = Random.Default
        val targets = DreamBigGame.pickTargets(pool, DreamBigGame.QUESTIONS_PER_LEVEL, random)
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

        activeDay = day
        roundQuestions = questions
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

    /** Soal berikutnya; soal terakhir → simpan progres + layar hasil. */
    fun next() {
        val q = _state.value.quiz ?: return
        if (q.selected == null) return // belum menjawab / double-tap Next
        val nextIndex = q.index + 1
        if (nextIndex >= q.total) {
            finishLevel(q)
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

    private fun finishLevel(q: DreamBigQuizUi) {
        val day = activeDay
        val stars = DreamBigGame.stars(q.score, q.total)
        val passed = q.score >= DreamBigGame.PASS_SCORE
        viewModelScope.launch {
            val updated = progressMutex.withLock {
                // Baca + tulis di IO: store.read() menyentuh disk.
                withContext(Dispatchers.IO) {
                    progressStore.read().withBest(day, q.score).also { progressStore.write(it) }
                }
            }
            progress = updated
            _state.update {
                it.copy(
                    mode = DreamBigMode.RESULT,
                    quiz = null,
                    levels = toLevelUi(levelData, updated),
                    resultDay = day,
                    resultScore = q.score,
                    resultStars = stars,
                    resultBestStreak = q.bestStreak,
                    resultPassed = passed,
                )
            }
        }
    }

    /** Ulangi level yang barusan selesai. */
    fun repeatLevel() = startLevel(activeDay)

    /** Buka level berikutnya (kalau ada & sudah lulus). */
    fun nextLevel() = startLevel(activeDay + 1)

    /** Kembali ke peta level. */
    fun backToLevels() {
        _state.update { it.copy(mode = DreamBigMode.LEVELS, quiz = null) }
    }

    // ---- Materi (transkrip) ----

    /** Buka materi hari [day]: daftar video hari itu, paragraf video pertama. */
    fun openTranscript(day: Int) {
        viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) {
                repository.videos().filter { it.day == day }
            }
            _state.update {
                it.copy(
                    mode = DreamBigMode.TRANSCRIPT,
                    transcriptVideos = videos,
                    transcriptVideo = videos.firstOrNull(),
                    transcriptParagraphs = emptyList(),
                )
            }
            videos.firstOrNull()?.let { openTranscriptVideo(it) }
        }
    }

    /** Buka transkrip satu video (hasil IO basi dibuang). */
    fun openTranscriptVideo(video: DreamBigVideo) {
        val requestId = ++transcriptRequestId
        viewModelScope.launch {
            val paragraphs = withContext(Dispatchers.IO) {
                DreamBigParser.paragraphs(repository.transcript(video).segments)
            }
            if (requestId != transcriptRequestId) return@launch
            _state.update {
                it.copy(transcriptVideo = video, transcriptParagraphs = paragraphs)
            }
        }
    }

    /** Kembali dari materi ke peta level. */
    fun backFromTranscript() {
        _state.update { it.copy(mode = DreamBigMode.LEVELS) }
    }

    // ---- Bantuan ----

    private fun toLevelUi(
        levels: List<DreamBigLevel>,
        progress: DreamBigProgress,
    ): List<DreamBigLevelUi> = levels.map { level ->
        val best = progress.best(level.day)
        DreamBigLevelUi(
            day = level.day,
            title = level.title,
            locked = !DreamBigGame.unlocked(level.day, progress.completedDays),
            bestScore = best,
            stars = DreamBigGame.stars(best, DreamBigGame.QUESTIONS_PER_LEVEL),
        )
    }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun dreamBigViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        DreamBigViewModel(
            repository = DreamBigRepository(app),
            vocabRepository = VocabularyRepository(app),
            progressStore = DreamBigProgressStore(app),
            settings = SettingsStore(app),
        )
    }
}
