package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opennur.tahsin.data.learning.MemorizationCard
import org.opennur.tahsin.data.learning.MemorizationEngine
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationHub
import org.opennur.tahsin.util.MemorizationSnapshot
import org.opennur.tahsin.util.MemorizationStore
import org.opennur.tahsin.util.SettingsStore

data class MemorizationUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val card: MemorizationCard? = null,
    val ayah: Ayah? = null,
    val revealed: Boolean = false,
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val error: Boolean = false,
)

/** Offline-first Hifz/Murajaah queue over the bundled Quran repository. */
@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val app: Context,
    private val repository: QuranRepository,
    private val store: MemorizationStore,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MemorizationUiState())
    val state: StateFlow<MemorizationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refreshLanguage() = refresh()

    fun reveal() {
        if (_state.value.card != null) _state.value = _state.value.copy(revealed = true)
    }

    fun remember() = answer(remembered = true)

    fun needReview() = answer(remembered = false)

    private fun answer(remembered: Boolean) {
        val card = _state.value.card ?: return
        val day = LocalDate.now().toEpochDay()
        val updated = if (remembered) {
            MemorizationEngine.remember(card, day)
        } else {
            MemorizationEngine.needReview(card, day)
        }
        viewModelScope.launch(Dispatchers.IO) {
            store.upsert(updated)
            if (remembered) {
                GamificationHub.award(app, Gamification.XP_MEMORIZATION_REVIEW)
            }
            load()
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = false)
        viewModelScope.launch(Dispatchers.IO) { load() }
    }

    private suspend fun load() {
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        try {
            val stored = store.read()
            val initial = if (stored.cards.isEmpty()) {
                val firstSurah = repository.fetchSurah(1, language)
                val cards = MemorizationEngine.startingCards(1, firstSurah.ayahCount)
                val snapshot = MemorizationSnapshot(cards)
                store.write(snapshot)
                snapshot
            } else {
                stored
            }
            val day = LocalDate.now().toEpochDay()
            val card = MemorizationEngine.selectNext(initial.cards, day)
            val ayah = card?.let { current ->
                repository.fetchSurah(current.surah, language).ayahs
                    .firstOrNull { it.number == current.ayah }
            }
            _state.value = MemorizationUiState(
                loading = false,
                language = language,
                card = card,
                ayah = ayah,
                revealed = false,
                dueCount = initial.cards.count { MemorizationEngine.isDue(it, day) },
                totalCount = initial.cards.size,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _state.value = MemorizationUiState(
                loading = false,
                language = language,
                error = true,
            )
        }
    }
}
