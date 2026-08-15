package org.opennur.tahsin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.opennur.tahsin.data.dreambig.DreamBigGame
import org.opennur.tahsin.data.learning.LearningGoal
import org.opennur.tahsin.data.learning.LearningPlanEngine
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.DreamBigProgressStore
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.LughohProgressStore
import org.opennur.tahsin.util.LearningPlanStore
import org.opennur.tahsin.util.ReadingHistoryEntry
import org.opennur.tahsin.util.ReadingHistoryStore
import org.opennur.tahsin.util.ReadingStatsStore
import org.opennur.tahsin.util.SettingsSource
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
    val dailyPlanCompleted: Int = 0,
    val dailyPlanTotal: Int = 0,
    // Riwayat baca (ayat terakhir yang dibuka, terbaru dulu)
    val history: List<ReadingHistoryEntry> = emptyList(),
    val surahNames: Map<Int, String> = emptyMap(),
)

/**
 * Statistik keseluruhan: agregasi semua challenge — Tahsin (baca Al-Qur'an),
 * Dream BIG (ronde kosakata), Belajar Arab (sesi latihan), Kosakata
 * (kata yang dikuasai), serta progres rencana harian. Sumber: store persisten
 * masing-masing fitur.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsStore: ReadingStatsStore,
    private val vocabStatsStore: VocabularyStatsStore,
    private val dreamBigStore: DreamBigProgressStore,
    private val lughohStore: LughohProgressStore,
    private val gamificationStore: GamificationStore,
    private val readingHistory: ReadingHistoryStore,
    private val learningPlanStore: LearningPlanStore,
    private val repository: QuranRepository,
    private val settings: SettingsSource,
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
            val goal = LearningGoal.fromKey(settings.learningGoalKey)
            val planKeys = LearningPlanEngine.taskTypesFor(goal).map { it.key }.toSet()
            val planSnapshot = learningPlanStore.read()
            val planCompleted = if (planSnapshot.day == today && planSnapshot.goalKey == goal.key) {
                planSnapshot.completedKeys.count { it in planKeys }
            } else {
                0
            }
            val planTotal = planKeys.size

            val tahsinAttempts = tahsin.sumOf { it.attempts }
            val tahsinBestPct = tahsin.maxOfOrNull { it.bestScore } ?: 0
            val dreamBestPct = dream.bestScore * 100 / DreamBigGame.QUESTIONS_PER_ROUND
            val lughohBestPct = lughoh.bestScore * 100 / LughohEngine.SESSION_SIZE
            val wordsMastered = vocab.cards.values.count { it.correctCount > 0 }
            val history = readingHistory.load()
            val names = repository.surahList().associate { it.number to it.nameLatin }

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
                history = history,
                surahNames = names,
                dailyPlanCompleted = planCompleted,
                dailyPlanTotal = planTotal,
            )
        }
    }

}
