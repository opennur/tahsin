package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.quran.Surah
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Bookmark
import org.opennur.tahsin.util.BookmarkStore
import org.opennur.tahsin.util.SettingsStore

/** Satu baris ayat favorit di layar daftar. */
data class FavoriteAyahUi(
    val surah: Int,
    val surahName: String,
    val ayah: Int,
    val arabic: String,
    val translation: String,
)

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.ID,
    val items: List<FavoriteAyahUi> = emptyList(),
)

/**
 * Layar Ayat Favorit: baca bookmark (BookmarkStore), lalu ambil teks Arab +
 * terjemahan tiap ayat dari QuranRepository (batch per surah, cache di VM).
 * Bahasa dibaca ulang tiap [refresh] — dipanggil tiap layar dibuka (VM
 * Activity-scoped, jadi jangan simpan bahasa hanya di init).
 */
class FavoritesViewModel(
    private val settings: SettingsStore,
    private val bookmarkStore: BookmarkStore,
    private val repository: QuranRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    fun refresh() {
        val lang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode }
            ?: AppLanguage.ID
        _state.update { it.copy(isLoading = true, language = lang) }
        viewModelScope.launch(Dispatchers.IO) {
            val bookmarks = bookmarkStore.load()
                .sortedWith(compareBy({ it.surah }, { it.ayah }))
            val names = repository.surahList().associate { it.number to it.nameLatin }
            val surahCache = HashMap<Int, Surah>()
            val items = bookmarks.mapNotNull { bookmark ->
                var surah = surahCache[bookmark.surah]
                if (surah == null) {
                    surah = runCatching { repository.fetchSurah(bookmark.surah, lang) }.getOrNull()
                    if (surah != null) surahCache[bookmark.surah] = surah
                }
                surah ?: return@mapNotNull null
                val ayah = surah.ayahs.firstOrNull { it.number == bookmark.ayah }
                    ?: return@mapNotNull null
                FavoriteAyahUi(
                    surah = bookmark.surah,
                    surahName = names[bookmark.surah] ?: "Surah ${bookmark.surah}",
                    ayah = bookmark.ayah,
                    arabic = ayah.text,
                    translation = ayah.translation,
                )
            }
            _state.update { it.copy(isLoading = false, items = items) }
        }
    }

    /** Hapus satu favorit (toggle) lalu muat ulang daftar. */
    fun remove(surah: Int, ayah: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkStore.toggle(Bookmark(surah, ayah))
            refresh()
        }
    }
}

/** Factory Android: semua dependensi diambil dari [context]. */
fun favoritesViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        FavoritesViewModel(
            settings = SettingsStore(app),
            bookmarkStore = BookmarkStore.fromContext(app),
            repository = QuranRepository(app),
        )
    }
}
