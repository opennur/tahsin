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
import org.opennur.tahsin.data.shorof.ShorofCatalog
import org.opennur.tahsin.data.shorof.ShorofChoiceExercise
import org.opennur.tahsin.data.shorof.ShorofEngine
import org.opennur.tahsin.data.shorof.ShorofExercise
import org.opennur.tahsin.data.shorof.ShorofLesson
import org.opennur.tahsin.data.shorof.ShorofRepository
import org.opennur.tahsin.data.shorof.ShorofSessionExercise
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.ShorofProgressStore
import org.opennur.tahsin.util.ShorofStats

enum class ShorofMode { HOME, LESSON, EXERCISES }

data class ShorofLessonUi(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val completed: Boolean,
)

data class ShorofLevelUi(
    val id: Int,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val lessons: List<ShorofLessonUi>,
)

data class ShorofUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val mode: ShorofMode = ShorofMode.HOME,
    val levels: List<ShorofLevelUi> = emptyList(),
    val stats: ShorofStats = ShorofStats(),
    val lesson: ShorofLesson? = null,
    val session: List<ShorofSessionExercise> = emptyList(),
    val exerciseIndex: Int = 0,
    val selected: Int? = null,
    val correct: Boolean? = null,
    val score: Int = 0,
    val total: Int = 0,
    val exercisesDone: Boolean = false,
) {
    val exercise: ShorofExercise? get() = session.getOrNull(exerciseIndex)?.exercise
}

@HiltViewModel
class ShorofViewModel @Inject constructor(
    private val app: Context,
    private val repository: ShorofRepository,
    private val progressStore: ShorofProgressStore,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ShorofUiState())
    val state: StateFlow<ShorofUiState> = _state.asStateFlow()
    private val progressMutex = Mutex()
    private val random = Random.Default
    private var catalog = ShorofCatalog(0, emptyList())
    private var stats = ShorofStats()

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
        _state.update { it.copy(mode = ShorofMode.LESSON, lesson = lesson) }
    }

    fun backToHome() = _state.update { it.copy(mode = ShorofMode.HOME, lesson = null) }

    fun startExercises() {
        val session = ShorofEngine.buildSession(allLessons(), ShorofEngine.SESSION_SIZE, random)
        if (session.isEmpty()) return
        _state.update {
            it.copy(
                mode = ShorofMode.EXERCISES,
                session = session,
                exerciseIndex = 0,
                selected = null,
                correct = null,
                score = 0,
                total = session.size,
                exercisesDone = false,
            )
        }
    }

    fun answerChoice(index: Int) {
        val state = _state.value
        val exercise = state.exercise as? ShorofChoiceExercise ?: return
        if (state.selected != null) return
        val correct = ShorofEngine.isChoiceCorrect(exercise, index)
        _state.update {
            it.copy(selected = index, correct = correct, score = it.score + if (correct) 1 else 0)
        }
    }

    fun next() {
        val state = _state.value
        if (state.exercisesDone || state.correct == null) return
        val nextIndex = state.exerciseIndex + 1
        if (nextIndex >= state.total) {
            finish(state)
        } else {
            _state.update { it.copy(exerciseIndex = nextIndex, selected = null, correct = null) }
        }
    }

    fun restart() = startExercises()

    private fun finish(state: ShorofUiState) {
        val lessonIds = state.session.map { it.lessonId }.toSet()
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
                    val value = progressStore.read().withSession(state.score, lessonIds)
                    progressStore.write(value)
                    GamificationHub.award(app, Gamification.XP_SHOROF_SESSION)
                    value
                }
            }
            stats = updated
            _state.update { it.copy(stats = updated, levels = toLevelUi(catalog, updated)) }
        }
    }

    private fun language(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.code == settings.languageCode } ?: AppLanguage.ID

    private fun allLessons(): List<ShorofLesson> = catalog.levels.flatMap { it.lessons }

    private fun toLevelUi(value: ShorofCatalog, current: ShorofStats): List<ShorofLevelUi> =
        value.levels.map { level ->
            ShorofLevelUi(
                id = level.id,
                titleId = level.titleId,
                titleEn = level.titleEn,
                titleAr = level.titleAr,
                lessons = level.lessons.map { lesson ->
                    ShorofLessonUi(
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
