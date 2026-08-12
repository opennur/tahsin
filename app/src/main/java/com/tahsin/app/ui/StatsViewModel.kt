package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.quran.QuranRepository
import com.tahsin.app.util.AyahStats
import com.tahsin.app.util.AppLanguage
import com.tahsin.app.util.ReadingStatsStore
import com.tahsin.app.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ringkasan statistik satu surah (agregat semua ayat yang dilatih). */
data class SurahStatsSummary(
    val number: Int,
    val nameLatin: String,
    val nameArabic: String,
    val attempts: Int,
    val avgScore: Int,
    val practicedAyahs: Int,
    val totalErrors: Int,
)

/** Satu baris "kata yang sering salah" pada layar statistik. */
data class WordErrorRow(
    val ayahNumber: Int,
    val word: String,
    val errorCount: Int,
)

/** State layar statistik & riwayat kesalahan. */
data class StatsState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val totalAttempts: Int = 0,
    val avgScore: Int = 0,
    val bestScore: Int = 0,
    val practicedAyahs: Int = 0,
    /** Ringkasan per surah (hanya yang pernah dilatih), urut nomor surah. */
    val surahs: List<SurahStatsSummary> = emptyList(),
    val selectedSurah: Int = 0,
    val selectedName: String = "",
    /** Kata yang sering salah pada surah terpilih (terurut paling sering). */
    val wordRows: List<WordErrorRow> = emptyList(),
    val showClearConfirm: Boolean = false,
)

/**
 * Statistik & riwayat kesalahan bacaan: ringkasan global, lalu "kata yang
 * sering salah" per surah. Sumber: [ReadingStatsStore] (persisten).
 */
class StatsViewModel(
    app: Context,
    private val repository: QuranRepository,
    private val statsStore: ReadingStatsStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    /** Muat ulang dari penyimpanan (dipanggil tiap layar dibuka). */
    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val surahList = repository.surahList()
            val all = statsStore.all()
            val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
                ?: AppLanguage.ID

            val summaries = all
                .groupBy { it.surahNumber }
                .map { (num, ayahs) ->
                    val meta = surahList.find { it.number == num }
                    val attempts = ayahs.sumOf { it.attempts }
                    SurahStatsSummary(
                        number = num,
                        nameLatin = meta?.nameLatin ?: "Surah $num",
                        nameArabic = meta?.nameArabic ?: "",
                        attempts = attempts,
                        avgScore = if (attempts > 0) ayahs.sumOf { it.scoreSum } / attempts else 0,
                        practicedAyahs = ayahs.size,
                        totalErrors = ayahs.sumOf { st -> st.wordErrors.sumOf { it.errorCount } },
                    )
                }
                .sortedBy { it.number }

            // Pertahankan surah yang sedang dipilih kalau masih ada; kalau tidak,
            // default ke surah yang paling sering dilatih.
            val selected = _state.value.selectedSurah
                .takeIf { n -> summaries.any { it.number == n } }
                ?: summaries.maxByOrNull { it.attempts }?.number
                ?: 0

            val totalAttempts = all.sumOf { it.attempts }
            _state.value = StatsState(
                isLoading = false,
                language = language,
                totalAttempts = totalAttempts,
                avgScore = if (totalAttempts > 0) all.sumOf { it.scoreSum } / totalAttempts else 0,
                bestScore = all.maxOfOrNull { it.bestScore } ?: 0,
                practicedAyahs = all.size,
                surahs = summaries,
                selectedSurah = selected,
                selectedName = summaries.find { it.number == selected }?.nameLatin.orEmpty(),
                wordRows = wordRowsFor(all, selected),
            )
        }
    }

    /** Pilih surah → tampilkan kata yang sering salah di surah itu. */
    fun selectSurah(number: Int) {
        val summary = _state.value.surahs.find { it.number == number } ?: return
        _state.update {
            it.copy(
                selectedSurah = number,
                selectedName = summary.nameLatin,
                wordRows = wordRowsFor(statsStore.all(), number),
            )
        }
    }

    fun requestClear() = _state.update { it.copy(showClearConfirm = true) }

    fun cancelClear() = _state.update { it.copy(showClearConfirm = false) }

    fun confirmClear() {
        statsStore.clear()
        _state.update { it.copy(showClearConfirm = false) }
        refresh()
    }

    /** Kata salah/terlewat semua ayat di surah, terurut paling sering (max 50). */
    private fun wordRowsFor(all: List<AyahStats>, surah: Int): List<WordErrorRow> =
        all.filter { it.surahNumber == surah }
            .flatMap { st -> st.wordErrors.map { WordErrorRow(st.ayahNumber, it.word, it.errorCount) } }
            .sortedWith(compareByDescending<WordErrorRow> { it.errorCount }.thenBy { it.ayahNumber })
            .take(MAX_ROWS_PER_SURAH)

    companion object {
        private const val MAX_ROWS_PER_SURAH = 50
    }
}

/** Factory manual DI (tanpa Hilt). */
fun statsViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        StatsViewModel(
            app = app,
            repository = QuranRepository(app),
            statsStore = ReadingStatsStore(app),
            settings = SettingsStore(app),
        )
    }
}
