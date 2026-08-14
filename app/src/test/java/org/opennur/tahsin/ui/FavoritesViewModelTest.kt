package org.opennur.tahsin.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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

/**
 * Tes FavoritesViewModel dengan MockK (mocking) + Turbine (koleksi StateFlow
 * per-emisi) + Truth (assertion readable). Ini pola baseline MVVM testable:
 * ViewModel dikonstruksi LANGSUNG dengan dependensi mock — tanpa Android,
 * tanpa Hilt — sehingga logika UI-state bisa diuji di JVM murni.
 *
 * Catatan Turbine + StateFlow: StateFlow bersifat conflating, jadi emisi
 * "isLoading = true" bisa terlewat jika pekerjaan IO selesai sangat cepat.
 * Karena itu tes memakai pola "skip sampai target" — deterministik apa pun
 * urutan emisinya.
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

    private fun settings(code: String) = mockk<SettingsSource> {
        every { languageCode } returns code
    }

    private fun repo(vararg surahs: Surah): QuranRepository = mockk {
        every { surahList() } returns surahs.toList()
        every { pagination() } returns MushafPagination(1, 0, emptyList(), emptyList())
        every { cachedSurahPlain(any()) } answers {
            surahs.firstOrNull { it.number == firstArg<Int>() }
        }
        coEvery { cachedSurah(any(), any()) } answers {
            surahs.firstOrNull { it.number == firstArg<Int>() }
        }
        coEvery { fetchSurah(any(), any()) } answers {
            surahs.firstOrNull { it.number == firstArg<Int>() }
                ?: error("fetchSurah(${firstArg<Int>()}) tidak ada di mock")
        }
        coEvery { searchIndex() } returns emptyList<SearchableAyah>()
    }

    private fun vm(vararg surahs: Surah, code: String = "id"): FavoritesViewModel =
        FavoritesViewModel(settings(code), bookmarkStore, repo(*surahs))

    @Test
    fun `refresh memuat bookmark terurut dan bahasa ID`() = runTest {
        val surah2 = Surah(2, "البقرة", "Al-Baqarah", 286, listOf(Ayah(255, "اللّٰهُ", "Allah.")))
        val viewModel = vm(surah2)
        bookmarkStore.toggle(Bookmark(2, 255))
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].surahName).isEqualTo("Al-Baqarah")
            assertThat(state.items[0].ayah).isEqualTo(255)
            assertThat(state.items[0].translation).isEqualTo("Allah.")
            assertThat(state.language).isEqualTo(AppLanguage.ID)
        }
    }

    @Test
    fun `refresh memuat terjemahan bahasa EN`() = runTest {
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "بِسْمِ", "In the name")))
        val viewModel = vm(surah1, code = "en")
        bookmarkStore.toggle(Bookmark(1, 1))
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].translation).isEqualTo("In the name")
            assertThat(state.language).isEqualTo(AppLanguage.EN)
        }
    }

    @Test
    fun `repo gagal untuk satu surah - item itu dilewati tanpa crash`() = runTest {
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "نص", "text")))
        val viewModel = vm(surah1)
        bookmarkStore.toggle(Bookmark(1, 1))
        bookmarkStore.toggle(Bookmark(2, 5)) // fetchSurah(2) error di mock
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].surah).isEqualTo(1)
        }
    }

    @Test
    fun `remove menghapus dari daftar dan store`() = runTest {
        val surah1 = Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "نص", "text")))
        val viewModel = vm(surah1)
        bookmarkStore.toggle(Bookmark(1, 1))
        viewModel.refresh()
        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
        }

        viewModel.remove(1, 1)
        viewModel.state.test {
            // Skip sampai daftar kosong (emisi perantara boleh terlewat).
            var state = awaitItem()
            while (state.items.isNotEmpty()) state = awaitItem()

            assertThat(state.items).isEmpty()
            assertThat(bookmarkStore.load()).isEmpty()
        }
    }

    @Test
    fun `tanpa bookmark - daftar kosong`() = runTest {
        val viewModel = vm()
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.items).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }
}
