package com.tahsin.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Tes penyimpanan progres game Dream BIG — file di direktori temp. */
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
    fun `read - file belum ada - progres default kosong`() {
        val p = store().read()
        assertTrue(p.bestScores.isEmpty())
        assertTrue(p.completedDays.isEmpty())
        assertEquals(0, p.best(3))
    }

    @Test
    fun `write & read - roundtrip lintas instance`() {
        store().write(DreamBigProgress(bestScores = mapOf(1 to 8, 2 to 5)))
        val p = store().read()
        assertEquals(8, p.best(1))
        assertEquals(5, p.best(2))
        assertEquals(setOf(1, 2), p.completedDays) // 8 & 5 ≥ PASS_SCORE(4)
        assertFalse(file.readText().isBlank())
    }

    @Test
    fun `completedDays - skor di bawah lulus tidak membuka level`() {
        val p = DreamBigProgress(bestScores = mapOf(1 to 3, 2 to 9))
        assertEquals(setOf(2), p.completedDays)
    }

    @Test
    fun `withBest - hanya menyimpan skor terbaik`() {
        val p = DreamBigProgress(bestScores = mapOf(1 to 6)).withBest(1, 4).withBest(1, 9)
        assertEquals(9, p.best(1))
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json{{{")
        val p = store().read()
        assertTrue(p.bestScores.isEmpty())
    }
}
