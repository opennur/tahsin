package org.opennur.tahsin.util

import java.io.File
import java.nio.file.Files
import android.content.Context
import android.content.ContextWrapper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShorofProgressStoreTest {
    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("shorof-progress-test").toFile()
        file = File(dir, "shorof-progress.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = ShorofProgressStore(file)

    @Test
    fun `default roundtrip dan withSession`() {
        assertEquals(ShorofStats(), store().read())
        val value = ShorofStats().withSession(5, setOf("1-1")).withSession(8, setOf("1-1", "2-1"))
        store().write(value)
        assertEquals(value, store().read())
        assertEquals(8, value.bestScore)
        assertEquals(2, value.sessionsPlayed)
        assertTrue(file.readText().isNotBlank())
    }

    @Test
    fun `file rusak null dan target invalid aman`() {
        file.writeText("bukan json")
        assertEquals(ShorofStats(), store().read())
        file.writeText("null")
        assertEquals(ShorofStats(), store().read())
        val parent = File(dir, "not-dir").apply { writeText("x") }
        ShorofProgressStore(File(parent, "target.json")).write(ShorofStats())
        ShorofProgressStore(File("shorof-relative.json")).writeDirect("{\"bestScore\":1}")
        File("shorof-relative.json").delete()
        val invalidParent = File(dir, "invalid-parent").apply { writeText("x") }
        ShorofProgressStore(File(invalidParent, "target.json")).write(ShorofStats())
        val targetDir = File(dir, "target-dir").apply {
            mkdirs()
            File(this, "placeholder").writeText("x")
        }
        ShorofProgressStore(targetDir).write(ShorofStats())
        ShorofProgressStore(dir).writeDirect("ignored")
    }

    @Test
    fun `fromContext memakai files directory`() {
        val context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = dir
        }
        assertEquals(ShorofStats(), ShorofProgressStore.fromContext(context).read())
    }
}
