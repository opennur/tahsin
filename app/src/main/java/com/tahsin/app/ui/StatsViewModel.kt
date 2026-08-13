package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.data.lughoh.LughohEngine
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.DreamBigProgressStore
import com.tahsin.app.util.LughohProgressStore
import com.tahsin.app.util.ReadingStatsStore
import com.tahsin.app.util.SettingsStore
import com.tahsin.app.util.VocabularyStatsStore
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
            )
        }
    }
}

/** Factory manual DI (tanpa Hilt) — pola sama seperti fitur lain. */
fun statsViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        StatsViewModel(
            statsStore = ReadingStatsStore(app),
            vocabStatsStore = VocabularyStatsStore(app),
            dreamBigStore = DreamBigProgressStore(app),
            lughohStore = LughohProgressStore(app),
            settings = SettingsStore(app),
        )
    }
}
