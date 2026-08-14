package org.opennur.tahsin.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tes penyimpanan bookmark ayat ([BookmarkStore]) — file di direktori temp
 * lewat konstruktor internal. Yang diuji: load kosong, toggle tambah/hapus,
 * baca lintas instance, urutan tersimpan, dan ketahanan file rusak.
 */
class BookmarkStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("bookmark-test").toFile()
        file = File(dir, "bookmarks.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `load - file belum ada - kosong`() {
        val store = BookmarkStore(file)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `toggle - tambah lalu hapus - set berubah`() {
        val store = BookmarkStore(file)
        val bm = Bookmark(2, 255)

        val afterAdd = store.toggle(bm)
        assertEquals(setOf(bm), afterAdd)

        val afterRemove = store.toggle(bm)
        assertTrue(afterRemove.isEmpty())
    }

    @Test
    fun `toggle - beberapa bookmark - set menyimpan semua`() {
        val store = BookmarkStore(file)
        store.toggle(Bookmark(1, 1))
        store.toggle(Bookmark(2, 255))
        store.toggle(Bookmark(36, 12))

        assertEquals(
            setOf(Bookmark(1, 1), Bookmark(2, 255), Bookmark(36, 12)),
            store.load(),
        )
    }

    @Test
    fun `baca lintas instance - tersimpan di disk`() {
        val first = BookmarkStore(file)
        first.toggle(Bookmark(67, 2))

        val second = BookmarkStore(file)
        assertEquals(setOf(Bookmark(67, 2)), second.load())
    }

    @Test
    fun `load - file rusak - kosong tidak crash`() {
        file.writeText("{bukan json!!!")
        val store = BookmarkStore(file)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `load - file JSON null - kosong`() {
        file.writeText("null")
        val store = BookmarkStore(file)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `toggle - urutan idempoten - tidak duplikat`() {
        val store = BookmarkStore(file)
        store.toggle(Bookmark(3, 30))
        store.toggle(Bookmark(3, 30))
        store.toggle(Bookmark(3, 30))
        assertEquals(setOf(Bookmark(3, 30)), store.load())
    }

    @Test
    fun `toggle - direktori tidak ada - tidak crash`() {
        // Tulis gagal (parent tidak ada) → cabang runCatching di writeToDisk.
        val missing = File(File(dir, "no-such-dir"), "bookmarks.json")
        val store = BookmarkStore(missing)
        store.toggle(Bookmark(1, 1))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `load - hanya surah dan ayah yang valid`() {
        val store = BookmarkStore(file)
        store.toggle(Bookmark(2, 255))
        store.toggle(Bookmark(0, 0))
        store.toggle(Bookmark(1, 0))
        store.toggle(Bookmark(0, 5))
        assertFalse(store.load().contains(Bookmark(0, 0)))
        assertFalse(store.load().contains(Bookmark(1, 0)))
        assertFalse(store.load().contains(Bookmark(0, 5)))
    }
}
