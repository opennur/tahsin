package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opennur.tahsin.data.nahwu.NahwuCatalog
import org.opennur.tahsin.data.nahwu.NahwuChoiceExercise
import org.opennur.tahsin.data.nahwu.NahwuEngine
import org.opennur.tahsin.data.nahwu.NahwuExercise
import org.opennur.tahsin.data.nahwu.NahwuLesson
import org.opennur.tahsin.data.nahwu.NahwuLevel
import org.opennur.tahsin.data.nahwu.NahwuRearrangeExercise
import org.opennur.tahsin.data.nahwu.NahwuSessionExercise
import org.opennur.tahsin.data.nahwu.NahwuWord
import org.opennur.tahsin.data.nahwu.NahwuRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.NahwuStats
import org.opennur.tahsin.util.NahwuProgressStore
import org.opennur.tahsin.util.SettingsStore

enum class NahwuMode { HOME, LESSON, EXERCISES }

data class NahwuLessonUi(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val completed: Boolean,
)

data class NahwuLevelUi(
    val id: Int,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val lessons: List<NahwuLessonUi>,
)

data class NahwuUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val mode: NahwuMode = NahwuMode.HOME,
    val levels: List<NahwuLevelUi> = emptyList(),
    val stats: NahwuStats = NahwuStats(),
    val lesson: NahwuLesson? = null,
    val session: List<NahwuSessionExercise> = emptyList(),
    val exerciseIndex: Int = 0,
    val selected: Int? = null,
    val correct: Boolean? = null,
    val shownWords: List<NahwuWord> = emptyList(),
    val tappedWords: List<Int> = emptyList(),
    val score: Int = 0,
    val total: Int = 0,
    val exercisesDone: Boolean = false,
) {
    val exercise: NahwuExercise? get() = session.getOrNull(exerciseIndex)?.exercise
    val answerReady: Boolean
        get() = when (val current = exercise) {
            null -> false
            is NahwuRearrangeExercise -> tappedWords.size == current.words.size
            else -> selected != null
        }
}

@HiltViewModel
class NahwuViewModel @Inject constructor(
    private val app: Context,
    private val repository: NahwuRepository,
    private val progressStore: NahwuProgressStore,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(NahwuUiState())
    val state: StateFlow<NahwuUiState> = _state.asStateFlow()
    private val progressMutex = Mutex()
    private val random = Random.Default
    private var catalog = NahwuCatalog(0, emptyList())
    private var stats = NahwuStats()

    init {
        viewModelScope.launch {
            val language = language()
            val loaded = withContext(Dispatchers.IO) { repository.catalog() to progressStore.read() }
            catalog = loaded.first
            stats = loaded.second
            _state.update {
                it.copy(
                    loading = false,
                    language = language,
                    levels = toLevelUi(catalog, stats),
                    stats = stats,
                )
            }
        }
    }

    fun refreshLanguage() {
        val language = language()
        if (_state.value.language != language) _state.update { it.copy(language = language) }
    }

    fun openLesson(id: String) {
        val lesson = allLessons().firstOrNull { it.id == id } ?: return
        _state.update { it.copy(mode = NahwuMode.LESSON, lesson = lesson) }
    }

    fun backToHome() = _state.update { it.copy(mode = NahwuMode.HOME, lesson = null) }

    fun startExercises() {
        val session = NahwuEngine.buildSession(allLessons(), NahwuEngine.SESSION_SIZE, random)
        if (session.isEmpty()) return
        val first = session.first().exercise
        _state.update {
            it.copy(
                mode = NahwuMode.EXERCISES,
                session = session,
                exerciseIndex = 0,
                selected = null,
                correct = null,
                shownWords = wordsFor(first),
                tappedWords = emptyList(),
                score = 0,
                total = session.size,
                exercisesDone = false,
            )
        }
    }

    fun answerChoice(index: Int) {
        val state = _state.value
        val exercise = state.exercise as? NahwuChoiceExercise ?: return
        if (state.selected != null) return
        val correct = NahwuEngine.isChoiceCorrect(exercise, index)
        _state.update {
            it.copy(selected = index, correct = correct, score = it.score + if (correct) 1 else 0)
        }
    }

    fun tapWord(index: Int) {
        val state = _state.value
        if (state.exercise !is NahwuRearrangeExercise || state.correct != null) return
        if (index !in state.shownWords.indices) return
        val tapped = state.tappedWords.toMutableList()
        if (index in tapped) tapped.remove(index)
        else if (tapped.size < state.shownWords.size) tapped.add(index)
        _state.update { it.copy(tappedWords = tapped) }
    }

    fun checkWords() {
        val state = _state.value
        val exercise = state.exercise as? NahwuRearrangeExercise ?: return
        if (state.correct != null || !state.answerReady) return
        val correct = NahwuEngine.isRearrangeCorrect(exercise, state.shownWords, state.tappedWords)
        _state.update {
            it.copy(correct = correct, score = it.score + if (correct) 1 else 0)
        }
    }

    fun next() {
        val state = _state.value
        if (state.exercisesDone || state.correct == null) return
        val nextIndex = state.exerciseIndex + 1
        if (nextIndex >= state.total) {
            finish(state)
            return
        }
        val nextExercise = state.session[nextIndex].exercise
        _state.update {
            it.copy(
                exerciseIndex = nextIndex,
                selected = null,
                correct = null,
                shownWords = wordsFor(nextExercise),
                tappedWords = emptyList(),
            )
        }
    }

    fun restart() = startExercises()

    private fun finish(state: NahwuUiState) {
        val completedLessonIds = state.session.map { it.lessonId }.toSet()
        _state.update {
            it.copy(
                exercisesDone = true,
                session = emptyList(),
                exerciseIndex = 0,
                selected = null,
                correct = null,
                shownWords = emptyList(),
                tappedWords = emptyList(),
            )
        }
        viewModelScope.launch {
            val updated = progressMutex.withLock {
                withContext(Dispatchers.IO) {
                    val value = progressStore.read().withSession(state.score, completedLessonIds)
                    progressStore.write(value)
                    GamificationHub.award(app, Gamification.XP_NAHWU_SESSION)
                    value
                }
            }
            stats = updated
            _state.update { it.copy(stats = updated, levels = toLevelUi(catalog, updated)) }
        }
    }

    private fun language(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.code == settings.languageCode } ?: AppLanguage.ID

    private fun allLessons(): List<NahwuLesson> = catalog.levels.flatMap(NahwuLevel::lessons)

    private fun wordsFor(exercise: NahwuExercise): List<NahwuWord> =
        if (exercise is NahwuRearrangeExercise) NahwuEngine.shuffleWords(exercise, random) else emptyList()

    private fun toLevelUi(value: NahwuCatalog, current: NahwuStats): List<NahwuLevelUi> =
        value.levels.map { level ->
            NahwuLevelUi(
                id = level.id,
                titleId = level.titleId,
                titleEn = level.titleEn,
                titleAr = level.titleAr,
                lessons = level.lessons.map { lesson ->
                    NahwuLessonUi(
                        id = lesson.id,
                        titleId = lesson.titleId,
                        titleEn = lesson.titleEn,
                        titleAr = lesson.titleAr,
                        completed = lesson.id in current.completedLessonIds,
                    )
                },
            )
        }
}
