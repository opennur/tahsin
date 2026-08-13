package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.util.Achievements
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.BadgeDef
import com.tahsin.app.util.BadgeProgress
import com.tahsin.app.util.Gamification
import com.tahsin.app.util.GamificationHub
import com.tahsin.app.util.GamificationStore
import com.tahsin.app.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Satu badge di layar: definisi + progres tier saat ini. */
data class BadgeUi(
    val def: BadgeDef,
    val progress: BadgeProgress,
)

data class BadgesUiState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val badges: List<BadgeUi> = emptyList(),
    val earnedCount: Int = 0,
    val totalCount: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
)

/**
 * Layar Penghargaan: evaluasi ulang badge (mungkin ada yang baru terpenuhi
 * sejak terakhir dibuka), lalu tampilkan daftar diraih/terkunci + ringkasan
 * level/XP. I/O disk di Dispatchers.IO.
 */
class BadgesViewModel(
    private val app: Context,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BadgesUiState())
    val state: StateFlow<BadgesUiState> = _state.asStateFlow()

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val g = GamificationStore.fromContext(app)
            GamificationHub.checkAndUnlock(app, g)
            val stats = g.read()
            val profile = GamificationHub.loadProfile(app, stats)
            val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID
            _state.value = BadgesUiState(
                isLoading = false,
                language = lang,
                badges = Achievements.ALL.map { badge ->
                    BadgeUi(
                        def = badge,
                        progress = Achievements.progressFor(
                            badge,
                            profile,
                            stats.badgeTiers[badge.key] ?: 0,
                        ),
                    )
                },
                earnedCount = stats.badgeTiers.size,
                totalCount = Achievements.ALL.size,
                xp = stats.xp,
                level = Gamification.levelFor(stats.xp),
            )
        }
    }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun badgesViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        BadgesViewModel(app = app, settings = SettingsStore(app))
    }
}
