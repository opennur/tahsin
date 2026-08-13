package com.tahsin.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Tes penyimpanan statistik arcade Belajar Arab — file di direktori temp. */
class LughohProgressStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("lughoh-progress-test").toFile()
        file = File(dir, "lughoh-progress.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = LughohProgressStore(file)

    @Test
    fun `read - file belum ada - statistik default kosong`() {
        val p = store().read()
        assertEquals(0, p.bestScore)
        assertEquals(0, p.roundsPlayed)
    }

    @Test
    fun `write & read - roundtrip lintas instance`() {
        store().write(LughohStats(bestScore = 7, roundsPlayed = 4))
        assertEquals(LughohStats(bestScore = 7, roundsPlayed = 4), store().read())
        assertFalse(file.readText().isBlank())
    }

    @Test
    fun `withRound - skor terbaik & jumlah sesi bertambah`() {
        val p = LughohStats().withRound(6).withRound(9).withRound(5)
        assertEquals(9, p.bestScore)
        assertEquals(3, p.roundsPlayed)
    }

    @Test
    fun `read - file lama format pelajaran selesai dibaca sebagai default`() {
        // File era pelajaran: {"completedLessonIds":["1-1"]} — field tak dikenal diabaikan.
        file.writeText("""{"completedLessonIds":["1-1","2-3"]}""")
        assertEquals(LughohStats(), store().read())
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json{{{")
        assertEquals(LughohStats(), store().read())
    }


    @Test
    fun `write - target berupa direktori - tidak crash`() {
        file.mkdirs()
        java.io.File(file, "placeholder").writeText("x") // dir tidak kosong → renameTo pasti gagal
        store().write(LughohStats(bestScore = 1, roundsPlayed = 1))
        assertTrue(file.isDirectory)
    }


    @Test
    fun `read - json literal null - keadaan default`() {
        file.writeText("null")
        assertEquals(LughohStats(), store().read())
    }


    @Test
    fun `writeDirect - file biasa - tersimpan`() {
        store().writeDirect("""{"bestScore":1}""")
        assertTrue(file.readText().contains("1"))
    }
}
