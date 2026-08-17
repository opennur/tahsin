package org.opennur.tahsin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.JuzStatusRow
import org.opennur.tahsin.util.PageStatusRow
import org.opennur.tahsin.util.PetaKhatamEngine
import org.opennur.tahsin.util.PetaKhatamSummary
import org.opennur.tahsin.util.SettingsSource
import org.opennur.tahsin.util.StatsStores

data class PetaKhatamUiState(
    val loading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val pageStatuses: List<PageStatusRow> = emptyList(),
    val juzStatuses: List<JuzStatusRow> = emptyList(),
    val juzStartPages: Map<Int, Int> = emptyMap(),
    val summary: PetaKhatamSummary = PetaKhatamSummary(0, 0, 0, 0),
    val viewMode: String = "pages",
)

@HiltViewModel
class PetaKhatamViewModel @Inject constructor(
    private val stores: StatsStores,
    private val repository: QuranRepository,
    private val settings: SettingsSource,
) : ViewModel() {

    private val _state = MutableStateFlow(PetaKhatamUiState())
    val state: StateFlow<PetaKhatamUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refreshLanguage() = refresh()

    fun refresh() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch(Dispatchers.IO) { load() }
    }

    fun setViewMode(mode: String) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    private fun load() {
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        val stats = stores.readingStats.all()
        val pagination = repository.pagination()

        val pageStatuses = PetaKhatamEngine.pageStatuses(stats, pagination)
        val juzStatuses = PetaKhatamEngine.juzStatuses(stats, pagination)
        val juzStartPages = pagination.juzStarts.associate { start ->
            start.juz to (pagination.pageOf(start.surah, start.ayah) ?: 1)
        }
        val summary = PetaKhatamEngine.summary(pageStatuses)

        _state.value = PetaKhatamUiState(
            loading = false,
            language = language,
            pageStatuses = pageStatuses,
            juzStatuses = juzStatuses,
            juzStartPages = juzStartPages,
            summary = summary,
            viewMode = _state.value.viewMode,
        )
    }
}
