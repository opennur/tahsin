package org.opennur.tahsin.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.quran.Surah
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.Bookmark
import org.opennur.tahsin.util.BookmarkStore
import org.opennur.tahsin.util.SearchableAyah
import org.opennur.tahsin.util.SettingsSource
import java.io.File
import java.nio.file.Files

/** Tes logika FavoritesViewModel dengan fake repository + store file temp. */
class FavoritesViewModelTest {

    private lateinit var dir: File
    private lateinit var bookmarkStore: BookmarkStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dir = Files.createTempDirectory("favorites-vm-test").toFile()
        bookmarkStore = BookmarkStore(File(dir, "bookmarks.json"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        dir.deleteRecursively()
    }

    private fun settings(code: String) = object : SettingsSource {
        override val languageCode: String = code
    }

    private fun fakeRepo(vararg surahs: Surah): QuranRepository = object : QuranRepository {
        override fun surahList(): List<Surah> = surahs.toList()
        override fun pagination(): MushafPagination =
            MushafPagination(1, 0, emptyList(), emptyList())
        override fun cachedSurahPlain(number: Int): Surah? =
            surahs.firstOrNull { it.number == number }
        override suspend fun cachedSurah(number: Int, lang: AppLanguage): Surah? =
            surahs.firstOrNull { it.number == number }
        override suspend fun fetchSurah(number: Int, lang: AppLanguage): Surah =
            surahs.firstOrNull { it.number == number }
                ?: error("fetchSurah($number) tidak ada di fake")
        override suspend fun searchIndex(): List<SearchableAyah> = emptyList()
    }

    private fun awaitState(
        state: StateFlow<FavoritesUiState>,
        extra: (FavoritesUiState) -> Boolean = { true },
    ): FavoritesUiState = runBlocking {
        withTimeout(5_000) { state.first { !it.isLoading && extra(it) } }
    }

    @Test
    fun `refresh memuat bookmark terurut dan bahasa ID`() {
        val surah2 = Surah(2, "البقرة", "Al-Baqarah", 286, listOf(Ayah(255, "اللّٰهُ", "Allah.")))
        val vm = FavoritesViewModel(settings("id"), bookmarkStore, fakeRepo(surah2))
        bookmarkStore.toggle(Bookmark(2, 255))
        vm.refresh()

        val state = awaitState(vm.state)

        assertEquals(1, state.items.size)
        assertEquals("Al-Baqarah", state.items[0].surahName)
        assertEquals(255, state.items[0].ayah)
        assertEquals("Allah.", state.items[0].translation)
        assertEquals(AppLanguage.ID, state.language)
    }

    @Test
    fun `refresh memuat terjemahan bahasa EN`() {
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "بِسْمِ", "In the name")))
        val vm = FavoritesViewModel(settings("en"), bookmarkStore, fakeRepo(surah1))
        bookmarkStore.toggle(Bookmark(1, 1))
        vm.refresh()

        val state = awaitState(vm.state)

        assertEquals("In the name", state.items[0].translation)
        assertEquals(AppLanguage.EN, state.language)
    }

    @Test
    fun `repo gagal untuk satu surah - item itu dilewati tanpa crash`() {
        // Fake hanya punya surah 1; bookmark surah 2 → fetchSurah(2) error.
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "نص", "text")))
        val vm = FavoritesViewModel(settings("id"), bookmarkStore, fakeRepo(surah1))
        bookmarkStore.toggle(Bookmark(1, 1))
        bookmarkStore.toggle(Bookmark(2, 5))
        vm.refresh()

        val state = awaitState(vm.state)

        assertEquals(1, state.items.size)
        assertEquals(1, state.items[0].surah)
    }

    @Test
    fun `remove menghapus dari daftar dan store`() {
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "نص", "text")))
        val vm = FavoritesViewModel(settings("id"), bookmarkStore, fakeRepo(surah1))
        bookmarkStore.toggle(Bookmark(1, 1))
        vm.refresh()
        awaitState(vm.state)

        vm.remove(1, 1)
        val state = awaitState(vm.state) { it.items.isEmpty() }

        assertTrue(state.items.isEmpty())
        assertTrue(bookmarkStore.load().isEmpty())
    }

    @Test
    fun `tanpa bookmark - daftar kosong`() {
        val vm = FavoritesViewModel(settings("id"), bookmarkStore, fakeRepo())
        vm.refresh()
        val state = awaitState(vm.state)
        assertTrue(state.items.isEmpty())
    }
}
