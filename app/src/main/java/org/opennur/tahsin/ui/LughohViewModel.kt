package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.opennur.tahsin.data.lughoh.Exercise
import org.opennur.tahsin.data.lughoh.LughohCatalog
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.data.lughoh.LughohLesson
import org.opennur.tahsin.data.lughoh.LughohRepository
import org.opennur.tahsin.data.lughoh.LughohEngine.forLanguage
import org.opennur.tahsin.data.lughoh.RearrangeExercise
import org.opennur.tahsin.data.lughoh.WordChip
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.LughohProgressStore
import org.opennur.tahsin.util.LughohStats
import org.opennur.tahsin.util.SettingsStore
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
    val titleEn: String = "",
    val titleAr: String,
)

/** Satu level di daftar materi. */
data class LughohLevelUi(
    val id: Int,
    val titleId: String,
    val titleEn: String = "",
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
    val session: List<Exercise> = emptyList(),
    val exerciseIndex: Int = 0,
    val selected: String? = null,
    val correct: Boolean? = null,
    val rearrangeShown: List<WordChip> = emptyList(),
    val rearrangeTapped: List<Int> = emptyList(),
    val score: Int = 0,
    val total: Int = 0,
    val exercisesDone: Boolean = false,
) {
    /** Latihan yang sedang tampil (dari sesi acak; null kalau sesi habis). */
    val exercise: Exercise? get() = session.getOrNull(exerciseIndex)

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
@HiltViewModel
class LughohViewModel @Inject constructor(
    private val app: Context,
    private val repository: LughohRepository,
    private val progressStore: LughohProgressStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LughohUiState())
    val state: StateFlow<LughohUiState> = _state.asStateFlow()

    private val progressMutex = Mutex()
    private val random = Random.Default

    /** Sesi mentah (tanpa resolusi bahasa) — untuk re-resolusi saat bahasa ganti. */
    private var rawSession: List<Exercise> = emptyList()

    private var catalog: LughohCatalog = LughohCatalog(schemaVersion = 0, levels = emptyList())
    private var stats: LughohStats = LughohStats()

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
        rawSession = LughohEngine.buildRandomSession(allLessons, LughohEngine.SESSION_SIZE, random)
        // Resolusi bahasa: opsi/prompt mengikuti bahasa aktif (ID atau EN).
        val session = rawSession.map { it.forLanguage(s.language, random) }
        if (session.isEmpty()) return
        val first = session.first()
        _state.update {
            it.copy(
                mode = LughohMode.EXERCISES,
                session = session,
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

    /** Latihan berikutnya; latihan terakhir → tandai selesai lalu simpan statistik. */
    fun next() {
        val s = _state.value
        if (s.exercisesDone) return
        if (s.correct == null) return // belum dinilai / double-tap
        val nextIndex = s.exerciseIndex + 1
        if (nextIndex >= s.total) {
            finishExercises(s)
        } else {
            val nextEx = s.session.getOrNull(nextIndex) ?: return
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

    /**
     * Sesi selesai: transisi ke hasil dilakukan SINKRON (sebelum IO) supaya
     * double-tap tidak menulis statistik dua kali; coroutine hanya menyimpan
     * skor lalu memperbarui rekor di state.
     */
    private fun finishExercises(s: LughohUiState) {
        val score = s.score
        _state.update {
            it.copy(
                exercisesDone = true,
                session = emptyList(),
                exerciseIndex = 0,
                selected = null,
                correct = null,
            )
        }
        viewModelScope.launch {
            val updated = progressMutex.withLock {
                withContext(Dispatchers.IO) {
                    val updatedStats = progressStore.read().withRound(score).also { progressStore.write(it) }
                    GamificationHub.award(app, Gamification.XP_LUGHOH_SESSION)
                    updatedStats
                }
            }
            stats = updated
            _state.update { it.copy(stats = updated) }
        }
    }

    /** Ulangi sesi latihan acak. */
    fun restartExercises() = startRandomExercises()

    /**
     * Sinkronkan bahasa terbaru dari pengaturan (VM di-cache per Activity) —
     * dipanggil setiap layar terbuka. Materi memakai teks id/en sesuai bahasa.
     * Sesi arcade di-resolusi ulang ke bahasa baru: dari latihan BELUM
     * terjawab (kalau yang sekarang sudah dinilai, biarkan — skor sudah
     * dihitung dan umpan balik jangan berubah).
     */
    fun refreshLanguage() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val s = _state.value
        if (s.language == lang) return
        val reResolveFrom = if (s.selected == null && s.correct == null) 0 else s.exerciseIndex + 1
        val reResolve = s.mode == LughohMode.EXERCISES && !s.exercisesDone &&
            reResolveFrom < rawSession.size
        _state.update {
            it.copy(
                language = lang,
                session = if (reResolve) {
                    rawSession.mapIndexed { i, ex ->
                        if (i >= reResolveFrom) ex.forLanguage(lang, random) else it.session.getOrNull(i) ?: ex
                    }
                } else {
                    it.session
                },
            )
        }
    }

    /** Kembali ke halaman awal (dari hasil sesi). */
    fun backToLesson() = backToHome()

    private fun toLevelUi(catalog: LughohCatalog): List<LughohLevelUi> =
        catalog.levels.map { level ->
            LughohLevelUi(
                id = level.id,
                titleId = level.titleId,
                titleEn = level.titleEn,
                titleAr = level.titleAr,
                lessons = level.lessons.map { lesson ->
                    LughohLessonUi(
                        id = lesson.id,
                        titleId = lesson.titleId,
                        titleEn = lesson.titleEn,
                        titleAr = lesson.titleAr,
                    )
                },
            )
        }
}
