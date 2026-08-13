package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.SettingsStore
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ringkasan ekonomi game untuk header (Home) & ringkasan (Statistik):
 * level, XP, streak, XP hari ini, dan badge terbaru yang diraih.
 */
data class GamificationUiState(
    val isLoading: Boolean = true,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val todayXp: Int = 0,
    val dailyGoalXp: Int = Gamification.DAILY_GOAL_XP,
    val latestBadgeKey: String? = null,
    val latestBadgeTier: Int = 0,
    val earnedBadgeCount: Int = 0,
)

/**
 * Memuat ringkasan gamification dari [GamificationStore]. Dipakai layar
 * Home (header) — dibaca ulang setiap Home masuk komposisi (setelah pop).
 */
class GamificationViewModel(
    private val app: Context,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(GamificationUiState())
    val state: StateFlow<GamificationUiState> = _state.asStateFlow()

    /** Bahasa untuk pemetaan badge (dipakai header). */
    val language: AppLanguage
        get() = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val stats = GamificationStore.fromContext(app).read()
            val today = LocalDate.now().toEpochDay()
            _state.value = GamificationUiState(
                isLoading = false,
                xp = stats.xp,
                level = Gamification.levelFor(stats.xp),
                streak = stats.streak,
                todayXp = Gamification.todayXpFor(stats, today),
                latestBadgeKey = stats.badgeTiers.entries.lastOrNull()?.key,
                latestBadgeTier = stats.badgeTiers.entries.lastOrNull()?.value ?: 0,
                earnedBadgeCount = stats.badgeTiers.size,
            )
        }
    }
}

/** Factory manual DI (tanpa Hilt). */
fun gamificationViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        GamificationViewModel(app = app, settings = SettingsStore(app))
    }
}
