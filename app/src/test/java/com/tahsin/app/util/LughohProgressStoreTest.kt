package com.tahsin.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Tes penyimpanan progres Belajar Arab — file di direktori temp. */
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
    fun `read - file belum ada - progres default kosong`() {
        val p = store().read()
        assertTrue(p.completedLessonIds.isEmpty())
        assertFalse(p.isCompleted("1-1"))
    }

    @Test
    fun `write & read - roundtrip lintas instance`() {
        store().write(LughohProgress(completedLessonIds = setOf("1-1", "2-3")))
        val p = store().read()
        assertEquals(setOf("1-1", "2-3"), p.completedLessonIds)
        assertTrue(p.isCompleted("2-3"))
        assertFalse(p.isCompleted("3-5"))
        assertFalse(file.readText().isBlank())
    }

    @Test
    fun `withCompleted - menambah tanpa menghapus yang lain`() {
        val p = LughohProgress(completedLessonIds = setOf("1-1")).withCompleted("1-2").withCompleted("1-1")
        assertEquals(setOf("1-1", "1-2"), p.completedLessonIds)
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json{{{")
        val p = store().read()
        assertTrue(p.completedLessonIds.isEmpty())
    }
}
