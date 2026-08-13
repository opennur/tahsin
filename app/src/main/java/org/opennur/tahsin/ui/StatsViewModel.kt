package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.opennur.tahsin.data.dreambig.DreamBigGame
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.DreamBigProgressStore
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.LughohProgressStore
import org.opennur.tahsin.util.ReadingStatsStore
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.VocabularyStatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State layar statistik keseluruhan (gabungan semua challenge).
 */
data class StatsState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    // Angka gabungan
    val totalSessions: Int = 0,
    val totalRounds: Int = 0,
    val bestScorePct: Int = 0,
    val wordsMastered: Int = 0,
    // Rincian ringkas per fitur
    val tahsinAttempts: Int = 0,
    val dreamBigRounds: Int = 0,
    val dreamBigBest: Int = 0,
    val lughohRounds: Int = 0,
    val lughohBest: Int = 0,
    // Ringkasan ekonomi game
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val todayXp: Int = 0,
    val dailyGoalXp: Int = Gamification.DAILY_GOAL_XP,
    val badgesCount: Int = 0,
    val latestBadgeKey: String? = null,
    val latestBadgeTier: Int = 0,
)

/**
 * Statistik keseluruhan: agregasi semua challenge — Tahsin (baca Al-Qur'an),
 * Dream BIG (ronde kosakata), Belajar Arab (sesi latihan), dan Kosakata
 * (kata yang dikuasai). Sumber: store persisten masing-masing fitur.
 */
class StatsViewModel(
    private val statsStore: ReadingStatsStore,
    private val vocabStatsStore: VocabularyStatsStore,
    private val dreamBigStore: DreamBigProgressStore,
    private val lughohStore: LughohProgressStore,
    private val gamificationStore: GamificationStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    /** Muat ulang dari penyimpanan (dipanggil tiap layar dibuka). */
    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val tahsin = statsStore.all()
            val vocab = vocabStatsStore.read()
            val dream = dreamBigStore.read()
            val lughoh = lughohStore.read()
            val gamification = gamificationStore.read()
            val today = java.time.LocalDate.now().toEpochDay()
            val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID

            val tahsinAttempts = tahsin.sumOf { it.attempts }
            val tahsinBestPct = tahsin.maxOfOrNull { it.bestScore } ?: 0
            val dreamBestPct = dream.bestScore * 100 / DreamBigGame.QUESTIONS_PER_ROUND
            val lughohBestPct = lughoh.bestScore * 100 / LughohEngine.SESSION_SIZE
            val wordsMastered = vocab.cards.values.count { it.correctCount > 0 }

            _state.value = StatsState(
                isLoading = false,
                language = language,
                totalSessions = tahsinAttempts + dream.roundsPlayed + lughoh.roundsPlayed,
                totalRounds = dream.roundsPlayed + lughoh.roundsPlayed,
                bestScorePct = maxOf(tahsinBestPct, dreamBestPct, lughohBestPct),
                wordsMastered = wordsMastered,
                tahsinAttempts = tahsinAttempts,
                dreamBigRounds = dream.roundsPlayed,
                dreamBigBest = dream.bestScore,
                lughohRounds = lughoh.roundsPlayed,
                lughohBest = lughoh.bestScore,
                xp = gamification.xp,
                level = Gamification.levelFor(gamification.xp),
                streak = gamification.streak,
                todayXp = Gamification.todayXpFor(gamification, today),
                badgesCount = gamification.badgeTiers.size,
                latestBadgeKey = gamification.badgeTiers.entries.lastOrNull()?.key,
                latestBadgeTier = gamification.badgeTiers.entries.lastOrNull()?.value ?: 0,
            )
        }
    }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun statsViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        StatsViewModel(
            statsStore = ReadingStatsStore.fromContext(app),
            vocabStatsStore = VocabularyStatsStore.fromContext(app),
            dreamBigStore = DreamBigProgressStore.fromContext(app),
            lughohStore = LughohProgressStore.fromContext(app),
            gamificationStore = GamificationStore.fromContext(app),
            settings = SettingsStore(app),
        )
    }
}
