package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.AyahSearch
import org.opennur.tahsin.util.SearchableAyah
import org.opennur.tahsin.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** State layar pencarian ayat. */
data class SearchState(
    val query: String = "",
    /** Indeks (114 surah) sedang dibangun pertama kali. */
    val indexing: Boolean = false,
    /** Query sedang diproses (debounce sudah lewat). */
    val searching: Boolean = false,
    val results: List<SearchableAyah> = emptyList(),
    val language: AppLanguage = AppLanguage.ID,
    /** Nama surah (number → nameLatin) untuk label hasil. */
    val surahNames: Map<Int, String> = emptyMap(),
)

/**
 * Pencarian ayat: kata Arab (ternormalisasi) atau kata kunci terjemahan ID/EN.
 * Indeks seluruh mushaf dibangun sekali (lazy, di IO) lalu dicari di memori;
 * ketikan di-debounce supaya tidak memproses tiap keystroke.
 */
class SearchViewModel(
    app: Context,
    private val repository: QuranRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var index: List<SearchableAyah>? = null
    private var searchJob: Job? = null
    /** Menjaga indeks hanya dibangun sekali meski ada beberapa pencarian serentak. */
    private val indexMutex = Mutex()

    init {
        val names = repository.surahList().associate { it.number to it.nameLatin }
        val language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        _state.value = SearchState(surahNames = names, language = language)
    }

    /** Sinkronkan bahasa terbaru dari pengaturan (VM di-cache per Activity). */
    fun refreshLanguage() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        if (_state.value.language == lang) return
        _state.update { it.copy(language = lang) }
    }

    /** Ubah kata kunci; pencarian dijalankan setelah debounce. */
    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query)
        }
    }

    private suspend fun runSearch(query: String) {
        _state.update { it.copy(searching = true) }
        val idx = ensureIndex()
        val results = withContext(Dispatchers.Default) { AyahSearch.search(idx, query) }
        // Terapkan hanya kalau query belum berubah selama pemrosesan.
        if (_state.value.query == query) {
            _state.update { it.copy(results = results, searching = false) }
        }
    }

    /**
     * Bangun indeks sekali (lazy) di thread IO; hasil di-cache di ViewModel.
     * Di-serialize dengan Mutex supaya tidak dibangun ganda, dan selalu
     * me-reset flag `indexing` (try/finally) walau dibatalkan/gagal.
     */
    private suspend fun ensureIndex(): List<SearchableAyah> {
        index?.let { return it }
        indexMutex.withLock {
            index?.let { return it }
            _state.update { it.copy(indexing = true) }
            try {
                val idx = repository.searchIndex()
                index = idx
                return idx
            } finally {
                _state.update { it.copy(indexing = false) }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}

/** Factory manual DI (tanpa Hilt). */
fun searchViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        SearchViewModel(
            app = app,
            repository = QuranRepository(app),
            settings = SettingsStore(app),
        )
    }
}
