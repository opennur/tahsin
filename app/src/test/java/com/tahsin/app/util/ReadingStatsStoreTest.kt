package com.tahsin.app.util

import com.tahsin.app.stt.AlignedWord
import com.tahsin.app.stt.WordStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tes penyimpanan persisten riwayat bacaan ([ReadingStatsStore]) — memakai
 * file di direktori temp (bukan Android Context) lewat konstruktor internal.
 *
 * Yang diuji: penulisan/gabungan per percobaan, pembacaan lintas instance,
 * urutan [all], [clear], dan ketahanan terhadap file rusak/kosong.
 */
class ReadingStatsStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("reading-stats-test").toFile()
        file = File(dir, "reading-stats.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = ReadingStatsStore(file)

    private fun correctWords(vararg words: String): List<AlignedWord> =
        words.mapIndexed { i, w -> AlignedWord(i, w, WordStatus.CORRECT, w) }

    // ---- kosong ----

    @Test
    fun `belum ada file - statsFor null dan all kosong`() {
        val s = store()
        assertNull(s.statsFor(1, 1))
        assertTrue(s.all().isEmpty())
    }

    @Test
    fun `file kosong atau daftar kosong - dibaca sebagai kosong`() {
        file.writeText("")
        assertNull(store().statsFor(1, 1))
        assertTrue(store().all().isEmpty())

        file.writeText("[]")
        assertNull(store().statsFor(1, 1))
    }

    // ---- record ----

    @Test
    fun `record - percobaan pertama tersimpan lengkap`() {
        store().record(1, 1, correctWords("a", "b"), listOf("a", "b"))

        val stats = store().statsFor(1, 1)
        assertEquals(1, stats!!.surahNumber)
        assertEquals(1, stats.ayahNumber)
        assertEquals(1, stats.attempts)
        assertEquals(100, stats.scoreSum)
        assertEquals(100, stats.bestScore)
        assertEquals(100, stats.lastScore)
        assertTrue(stats.wordErrors.isEmpty())
    }

    @Test
    fun `record - percobaan kedua digabung ke statistik yang sama`() {
        val s = store()
        s.record(1, 1, correctWords("a", "b"), listOf("a", "b")) // skor 100
        s.record(1, 1, listOf(
            AlignedWord(0, "a", WordStatus.CORRECT, "a"),
            AlignedWord(1, "b", WordStatus.MISMATCH, "x"),
        ), listOf("a", "b")) // skor 50

        val stats = s.statsFor(1, 1)!!
        assertEquals(2, stats.attempts)
        assertEquals(150, stats.scoreSum)
        assertEquals(75, stats.avgScore)
        assertEquals(100, stats.bestScore)
        assertEquals(50, stats.lastScore)
        assertEquals(WordError(1, "b", 1), stats.wordErrors.single())
    }

    @Test
    fun `record - beberapa surah tercatat dan all terurut surah lalu ayat`() {
        val s = store()
        s.record(2, 3, correctWords("a"), listOf("a"))
        s.record(1, 5, correctWords("a"), listOf("a"))
        s.record(1, 2, correctWords("a"), listOf("a"))

        val all = s.all()
        assertEquals(
            listOf(1 to 2, 1 to 5, 2 to 3),
            all.map { it.surahNumber to it.ayahNumber },
        )
    }

    @Test
    fun `statsFor untuk ayat yang belum pernah dilatih - null`() {
        store().record(1, 1, correctWords("a"), listOf("a"))
        assertNull(store().statsFor(1, 2))
        assertNull(store().statsFor(2, 1))
    }

    // ---- persistensi lintas instance ----

    @Test
    fun `hasil baru terlihat oleh instance lain (tanpa cache instance)`() {
        store().record(1, 2, correctWords("a", "b"), listOf("a", "b"))

        val other = store() // instance baru, file sama
        val stats = other.statsFor(1, 2)
        assertEquals(1, stats!!.attempts)
        assertEquals(100, stats.scoreSum)
    }

    // ---- clear ----

    @Test
    fun `clear - menghapus seluruh riwayat`() {
        val s = store()
        s.record(1, 1, correctWords("a"), listOf("a"))
        s.record(2, 2, correctWords("b"), listOf("b"))

        s.clear()

        assertTrue(s.all().isEmpty())
        assertNull(s.statsFor(1, 1))
        assertNull(s.statsFor(2, 2))
        assertTrue(!file.exists())
    }

    @Test
    fun `clear - aman dipanggil saat belum ada file`() {
        store().clear() // tidak crash
        assertTrue(store().all().isEmpty())
    }

    // ---- ketahanan file rusak ----

    @Test
    fun `file rusak - dibaca kosong, record berikutnya tetap berfungsi`() {
        file.writeText("{{{bukan json")

        val s = store()
        assertNull(s.statsFor(1, 1))
        assertTrue(s.all().isEmpty())

        // Penulisan berikutnya menimpa file rusak → riwayat normal kembali.
        s.record(1, 1, correctWords("a"), listOf("a"))
        assertEquals(1, s.statsFor(1, 1)!!.attempts)
    }

    @Test
    fun `file dengan wordErrors null - tidak crash dan menjadi daftar kosong`() {
        file.writeText(
            """[{"surahNumber":1,"ayahNumber":1,"attempts":2,"scoreSum":100,"bestScore":60,"lastScore":40,"lastPracticedAt":0,"wordErrors":null}]""",
        )
        val stats = store().statsFor(1, 1)
        assertEquals(2, stats!!.attempts)
        assertEquals(50, stats.avgScore)
        assertTrue(stats.wordErrors.isEmpty())
    }

    @Test
    fun `record - kata salah terlewat tercatat sebagai wordError`() {
        store().record(5, 9, listOf(
            AlignedWord(0, "rahman", WordStatus.MISMATCH, "rahmaan"),
            AlignedWord(1, "rahim", WordStatus.SKIPPED),
        ), listOf("rahman", "rahim"))

        val stats = store().statsFor(5, 9)!!
        assertEquals(0, stats.scoreSum) // tidak ada yang benar
        assertEquals(
            listOf(WordError(0, "rahman", 1), WordError(1, "rahim", 1)),
            stats.wordErrors,
        )
    }
}
