package org.opennur.tahsin.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Tes penyimpanan statistik arcade Dream BIG — file di direktori temp. */
class DreamBigProgressStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("dreambig-progress-test").toFile()
        file = File(dir, "dreambig-progress.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = DreamBigProgressStore(file)

    @Test
    fun `read - file belum ada - statistik default kosong`() {
        val p = store().read()
        assertEquals(0, p.bestScore)
        assertEquals(0, p.bestStreak)
        assertEquals(0, p.roundsPlayed)
    }

    @Test
    fun `write & read - roundtrip lintas instance`() {
        store().write(DreamBigStats(bestScore = 8, bestStreak = 5, roundsPlayed = 3))
        val p = store().read()
        assertEquals(DreamBigStats(bestScore = 8, bestStreak = 5, roundsPlayed = 3), p)
        assertFalse(file.readText().isBlank())
    }

    @Test
    fun `withRound - simpan skor & streak terbaik, ronde bertambah`() {
        val p = DreamBigStats()
            .withRound(score = 6, streak = 4)
            .withRound(score = 9, streak = 3)
            .withRound(score = 5, streak = 7)
        assertEquals(9, p.bestScore)
        assertEquals(7, p.bestStreak)
        assertEquals(3, p.roundsPlayed)
    }

    @Test
    fun `read - file lama format level dibaca sebagai default`() {
        // File era level: {"bestScores":{...}} — field tak dikenal diabaikan.
        file.writeText("""{"bestScores":{"1":8,"2":5}}""")
        assertEquals(DreamBigStats(), store().read())
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json{{{")
        assertEquals(DreamBigStats(), store().read())
    }


    @Test
    fun `write - target berupa direktori - tidak crash`() {
        file.mkdirs()
        java.io.File(file, "placeholder").writeText("x") // dir tidak kosong → renameTo pasti gagal
        store().write(DreamBigStats(bestScore = 1, bestStreak = 1, roundsPlayed = 1))
        assertTrue(file.isDirectory)
    }


    @Test
    fun `read - json literal null - keadaan default`() {
        file.writeText("null")
        assertEquals(DreamBigStats(), store().read())
    }


    @Test
    fun `writeDirect - file biasa - tersimpan`() {
        store().writeDirect("""{"bestScore":1}""")
        assertTrue(file.readText().contains("1"))
    }
}
