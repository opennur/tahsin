package org.opennur.tahsin.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tes penyimpanan riwayat baca ([ReadingHistoryStore]) — file di direktori
 * temp lewat konstruktor internal. Yang diuji: load kosong, record
 * tambah/urutan, dedup pindah ke depan, batas entri, baca lintas instance,
 * filter entri invalid, dan ketahanan file rusak.
 */
class ReadingHistoryStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("reading-history-test").toFile()
        file = File(dir, "reading_history.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `load kosong saat file belum ada`() {
        val store = ReadingHistoryStore(file)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `record menambah entri dengan urutan terbaru di depan`() {
        val store = ReadingHistoryStore(file)
        store.record(2, 255, 100L)
        store.record(1, 1, 200L)
        val history = store.load()
        assertEquals(listOf(
            ReadingHistoryEntry(1, 1, 200L),
            ReadingHistoryEntry(2, 255, 100L),
        ), history)
    }

    @Test
    fun `record mengunjungi ulang tidak menduplikat - dipindah ke depan`() {
        val store = ReadingHistoryStore(file)
        store.record(2, 255, 100L)
        store.record(1, 1, 200L)
        store.record(2, 255, 300L)
        val history = store.load()
        assertEquals(2, history.size)
        assertEquals(ReadingHistoryEntry(2, 255, 300L), history[0])
        assertEquals(ReadingHistoryEntry(1, 1, 200L), history[1])
    }

    @Test
    fun `record membatasi jumlah entri ke MAX_ENTRIES`() {
        val store = ReadingHistoryStore(file)
        for (i in 1..ReadingHistoryStore.MAX_ENTRIES + 5) {
            store.record(3, i, i.toLong())
        }
        val history = store.load()
        assertEquals(ReadingHistoryStore.MAX_ENTRIES, history.size)
        // Entri paling baru (3, MAX+5) ada di depan; yang tertua dibuang.
        assertEquals(ReadingHistoryEntry(3, ReadingHistoryStore.MAX_ENTRIES + 5, (ReadingHistoryStore.MAX_ENTRIES + 5).toLong()), history[0])
        assertTrue(history.none { it.ayah == 1 })
    }

    @Test
    fun `persisten lintas instance - record lalu load dari store baru`() {
        ReadingHistoryStore(file).record(67, 1, 500L)
        val fresh = ReadingHistoryStore(file)
        assertEquals(listOf(ReadingHistoryEntry(67, 1, 500L)), fresh.load())
    }

    @Test
    fun `load menyaring entri invalid`() {
        file.writeText(
            """[{"surah":0,"ayah":5,"timestamp":1},{"surah":1,"ayah":0,"timestamp":2},{"surah":2,"ayah":255,"timestamp":3}]""",
        )
        val store = ReadingHistoryStore(file)
        assertEquals(listOf(ReadingHistoryEntry(2, 255, 3L)), store.load())
    }

    @Test
    fun `file rusak tidak menyebabkan crash - load kosong`() {
        file.writeText("bukan json{{{")
        val store = ReadingHistoryStore(file)
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `file berisi null json - load kosong`() {
        file.writeText("null")
        val store = ReadingHistoryStore(file)
        assertTrue(store.load().isEmpty())
    }
}
