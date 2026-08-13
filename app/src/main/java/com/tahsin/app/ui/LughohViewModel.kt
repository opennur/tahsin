package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.lughoh.Exercise
import com.tahsin.app.data.lughoh.LughohCatalog
import com.tahsin.app.data.lughoh.LughohEngine
import com.tahsin.app.data.lughoh.LughohLesson
import com.tahsin.app.data.lughoh.LughohRepository
import com.tahsin.app.data.lughoh.RearrangeExercise
import com.tahsin.app.data.lughoh.WordChip
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.LughohProgressStore
import com.tahsin.app.util.LughohStats
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

/** Mode layar "Belajar Arab". */
enum class LughohMode { HOME, LESSON, EXERCISES }

/** Satu baris pelajaran di daftar materi (state tampilan). */
data class LughohLessonUi(
    val id: String,
    val titleId: String,
    val titleAr: String,
)

/** Satu level di daftar materi. */
data class LughohLevelUi(
    val id: Int,
    val titleId: String,
    val titleAr: String,
    val lessons: List<LughohLessonUi>,
)

/** State layar Belajar Arab. */
data class LughohUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val mode: LughohMode = LughohMode.HOME,
    val levels: List<LughohLevelUi> = emptyList(),
    /** Statistik arcade tersimpan (rekor & jumlah sesi). */
    val stats: LughohStats = LughohStats(),
    val lesson: LughohLesson? = null,
    // Sesi tadribat acak
    val exerciseIndex: Int = 0,
    val selected: String? = null,
    val correct: Boolean? = null,
    val rearrangeShown: List<WordChip> = emptyList(),
    val rearrangeTapped: List<Int> = emptyList(),
    val score: Int = 0,
    val total: Int = 0,
    val exercisesDone: Boolean = false,
) {
    /** Latihan yang sedang tampil (null kalau sesi habis). */
    val exercise: Exercise? get() = lesson?.tadribat?.getOrNull(exerciseIndex)

    /** Apakah jawaban sudah lengkap sehingga bisa diperiksa/dinilai. */
    val answerReady: Boolean
        get() = when (val ex = exercise) {
            null -> false
            is RearrangeExercise -> rearrangeTapped.size == ex.words.size
            else -> selected != null
        }
}

/**
 * Belajar Bahasa Arab (arcade): sesi latihan acak dari seluruh pelajaran —
 * bisa dimainkan terus; materi (dialog/kosakata/tata bahasa) tetap bisa
 * dibaca lewat browser level/pelajaran. Rekor tersimpan di [LughohProgressStore].
 */
class LughohViewModel(
    private val repository: LughohRepository,
    private val progressStore: LughohProgressStore,
    settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LughohUiState())
    val state: StateFlow<LughohUiState> = _state.asStateFlow()

    private val progressMutex = Mutex()
    private val random = Random.Default

    private var catalog: LughohCatalog = LughohCatalog(schemaVersion = 0, levels = emptyList())
    private var stats: LughohStats = LughohStats()
    private var activeLessonId = ""
    private var exercises: List<Exercise> = emptyList()

    init {
        viewModelScope.launch {
            val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID
            val (cat, st) = withContext(Dispatchers.IO) {
                repository.catalog() to progressStore.read()
            }
            catalog = cat
            stats = st
            _state.update {
                it.copy(
                    loading = false,
                    language = language,
                    levels = toLevelUi(cat),
                    stats = st,
                )
            }
        }
    }

    // ---- Navigasi ----

    /** Buka detail pelajaran [lessonId] (materi saja, tanpa latihan). */
    fun openLesson(lessonId: String) {
        val lesson = catalog.levels.asSequence()
            .flatMap { it.lessons.asSequence() }
            .firstOrNull { it.id == lessonId } ?: return
        activeLessonId = lessonId
        _state.update { it.copy(mode = LughohMode.LESSON, lesson = lesson) }
    }

    /** Kembali ke halaman awal (arcade + browser materi). */
    fun backToHome() {
        _state.update { it.copy(mode = LughohMode.HOME, lesson = null) }
    }

    // ---- Sesi latihan acak ----

    /** Mulai sesi latihan acak: [LughohEngine.SESSION_SIZE] latihan dari semua pelajaran. */
    fun startRandomExercises() {
        val s = _state.value
        val allLessons = catalog.levels.flatMap { it.lessons }
        val session = LughohEngine.buildRandomSession(allLessons, LughohEngine.SESSION_SIZE, random)
        if (session.isEmpty()) return
        // activeLessonId dipakai sebagai wadah sesi (bukan materi).
        activeLessonId = "random"
        exercises = session
        val first = session.first()
        _state.update {
            it.copy(
                mode = LughohMode.EXERCISES,
                exerciseIndex = 0,
                selected = null,
                correct = null,
                rearrangeShown = if (first is RearrangeExercise) {
                    LughohEngine.shuffleRearrange(first, random)
                } else emptyList(),
                rearrangeTapped = emptyList(),
                score = 0,
                total = session.size,
                exercisesDone = false,
            )
        }
    }

    /** Jawab latihan pilihan (langsung dinilai). */
    fun answerChoice(option: String) {
        val s = _state.value
        val ex = s.exercise ?: return
        if (ex is RearrangeExercise) return
        if (s.selected != null) return
        val correct = LughohEngine.isChoiceCorrect(ex, option)
        _state.update {
            it.copy(
                selected = option,
                correct = correct,
                score = it.score + if (correct) 1 else 0,
            )
        }
    }

    /** Ketuk kata (indeks di [LughohUiState.rearrangeShown]) pada latihan menyusun. */
    fun tapRearrangeChip(index: Int) {
        val s = _state.value
        val ex = s.exercise as? RearrangeExercise ?: return
        if (s.correct != null) return // sudah dinilai — jangan diubah
        if (index !in s.rearrangeShown.indices) return
        val tapped = s.rearrangeTapped.toMutableList()
        if (index in tapped) {
            tapped.remove(index)
        } else {
            if (tapped.size >= ex.words.size) return
            tapped.add(index)
        }
        _state.update { it.copy(rearrangeTapped = tapped) }
    }

    /** Periksa susunan kata pada latihan menyusun (setelah semua kata terpilih). */
    fun checkRearrange() {
        val s = _state.value
        val ex = s.exercise as? RearrangeExercise ?: return
        if (s.correct != null) return
        if (s.rearrangeTapped.size != ex.words.size) return
        val tappedChips = s.rearrangeTapped.map { s.rearrangeShown[it] }
        val correct = LughohEngine.isRearrangeCorrect(ex, tappedChips)
        _state.update {
            it.copy(
                correct = correct,
                score = it.score + if (correct) 1 else 0,
            )
        }
    }

    /** Latihan berikutnya; latihan terakhir → simpan statistik + hasil. */
    fun next() {
        val s = _state.value
        if (s.correct == null) return // belum dinilai / double-tap
        val nextIndex = s.exerciseIndex + 1
        if (nextIndex >= s.total) {
            finishExercises()
        } else {
            val nextEx = exercises[nextIndex]
            _state.update {
                it.copy(
                    exerciseIndex = nextIndex,
                    selected = null,
                    correct = null,
                    rearrangeShown = if (nextEx is RearrangeExercise) {
                        LughohEngine.shuffleRearrange(nextEx, random)
                    } else emptyList(),
                    rearrangeTapped = emptyList(),
                )
            }
        }
    }

    /** Semua latihan selesai → simpan statistik sesi (IO + mutex). */
    private fun finishExercises() {
        val s = _state.value
        viewModelScope.launch {
            val updated = progressMutex.withLock {
                withContext(Dispatchers.IO) {
                    progressStore.read().withRound(s.score).also { progressStore.write(it) }
                }
            }
            stats = updated
            _state.update {
                it.copy(exercisesDone = true, stats = updated)
            }
        }
    }

    /** Ulangi sesi latihan acak. */
    fun restartExercises() = startRandomExercises()

    /** Kembali ke halaman awal (dari hasil sesi). */
    fun backToLesson() = backToHome()

    private fun toLevelUi(catalog: LughohCatalog): List<LughohLevelUi> =
        catalog.levels.map { level ->
            LughohLevelUi(
                id = level.id,
                titleId = level.titleId,
                titleAr = level.titleAr,
                lessons = level.lessons.map { lesson ->
                    LughohLessonUi(
                        id = lesson.id,
                        titleId = lesson.titleId,
                        titleAr = lesson.titleAr,
                    )
                },
            )
        }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun lughohViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        LughohViewModel(
            repository = LughohRepository(app),
            progressStore = LughohProgressStore(app),
            settings = SettingsStore(app),
        )
    }
}
