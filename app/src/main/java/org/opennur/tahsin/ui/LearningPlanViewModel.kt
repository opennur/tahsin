package org.opennur.tahsin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.opennur.tahsin.data.learning.DailyLearningPlan
import org.opennur.tahsin.data.learning.LearningGoal
import org.opennur.tahsin.data.learning.LearningPlanEngine
import org.opennur.tahsin.data.learning.LearningTaskType
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.LearningPlanStore
import org.opennur.tahsin.util.SettingsStore

data class LearningPlanUiState(
    val loading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val language: AppLanguage = AppLanguage.ID,
    val goal: LearningGoal = LearningGoal.RECITATION,
    val dailyMinutes: Int = 15,
    val plan: DailyLearningPlan = LearningPlanEngine.build(
        day = 0,
        goal = LearningGoal.RECITATION,
        dailyMinutes = 15,
    ),
)

/** Owns onboarding preferences and the current day's deterministic task list. */
@HiltViewModel
class LearningPlanViewModel @Inject constructor(
    private val store: LearningPlanStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LearningPlanUiState())
    val state: StateFlow<LearningPlanUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = readState()
        }
    }

    fun saveOnboarding(goal: LearningGoal, dailyMinutes: Int) {
        settings.learningGoalKey = goal.key
        settings.dailyMinutes = dailyMinutes
        settings.onboardingComplete = true
        refresh()
    }

    fun complete(type: LearningTaskType) {
        val current = _state.value
        if (current.loading || !current.onboardingComplete || type !in current.plan.tasks.map { it.type }) return
        viewModelScope.launch(Dispatchers.IO) {
            store.markComplete(
                day = current.plan.day,
                goalKey = current.goal.key,
                taskKey = type.key,
            )
            _state.value = readState()
        }
    }

    private fun readState(): LearningPlanUiState {
        val day = LocalDate.now().toEpochDay()
        val goal = settings.learningGoal
        val snapshot = store.read()
        val completed = if (snapshot.day == day && snapshot.goalKey == goal.key) {
            snapshot.completedKeys
        } else {
            emptySet()
        }
        return LearningPlanUiState(
            loading = false,
            onboardingComplete = settings.onboardingComplete,
            language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID,
            goal = goal,
            dailyMinutes = settings.dailyMinutes,
            plan = LearningPlanEngine.build(
                day = day,
                goal = goal,
                completedKeys = completed,
                dailyMinutes = settings.dailyMinutes,
            ),
        )
    }
}
