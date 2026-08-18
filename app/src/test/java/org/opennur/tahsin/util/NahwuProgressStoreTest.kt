package org.opennur.tahsin.util

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NahwuProgressStoreTest {
    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("nahwu-progress-test").toFile()
        file = File(dir, "nahwu-progress.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = NahwuProgressStore(file)

    @Test
    fun `default dan roundtrip`() {
        assertEquals(NahwuStats(), store().read())
        val value = NahwuStats(8, 2, setOf("1-1"))
        store().write(value)
        assertEquals(value, store().read())
        assertTrue(file.readText().isNotBlank())
    }

    @Test
    fun `withSession menyimpan skor sesi dan pelajaran unik`() {
        val value = NahwuStats().withSession(5, setOf("1-1", "1-2")).withSession(8, setOf("1-2", "2-1"))
        assertEquals(8, value.bestScore)
        assertEquals(2, value.sessionsPlayed)
        assertEquals(setOf("1-1", "1-2", "2-1"), value.completedLessonIds)
    }

    @Test
    fun `file rusak atau null kembali default`() {
        file.writeText("bukan json")
        assertEquals(NahwuStats(), store().read())
        file.writeText("null")
        assertEquals(NahwuStats(), store().read())
    }

    @Test
    fun `target direktori dan writeDirect tidak crash`() {
        file.mkdirs()
        File(file, "placeholder").writeText("x")
        store().write(NahwuStats(bestScore = 1))
        assertTrue(file.isDirectory)
        val direct = File(dir, "direct.json")
        NahwuProgressStore(direct).writeDirect("{\"bestScore\":2}")
        assertTrue(direct.readText().contains("2"))
    }

    @Test
    fun `write dengan file tanpa parent ditelan`() {
        NahwuProgressStore(File("nahwu-progress-relative.json")).write(NahwuStats(bestScore = 1))
        File("nahwu-progress-relative.json").delete()
        val invalidParent = File(dir, "not-a-directory").apply { writeText("x") }
        NahwuProgressStore(File(invalidParent, "target.json")).write(NahwuStats(bestScore = 1))
    }

    @Test
    fun `fromContext memakai application files directory`() {
        val context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = dir
        }
        assertEquals(NahwuStats(), NahwuProgressStore.fromContext(context).read())
    }

}
