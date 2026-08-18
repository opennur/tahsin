package org.opennur.tahsin.data.nahwu

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Memastikan aset Nahwu yang dibundel benar-benar bisa diparse. */
class NahwuAssetsIntegrationTest {
    private val assetsRoot: File by lazy {
        listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File(System.getProperty("user.dir"), "src/main/assets"),
            File(System.getProperty("user.dir"), "app/src/main/assets"),
        ).firstOrNull { File(it, "nahwu/lessons.json").isFile }
            ?: error("aset Nahwu tidak ditemukan")
    }

    @Test
    fun `aset asli memiliki delapan pelajaran dan latihan lengkap`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        assertEquals(1, catalog.schemaVersion)
        assertEquals(3, catalog.levels.size)
        assertEquals(8, catalog.levels.sumOf { it.lessons.size })
        catalog.levels.flatMap { it.lessons }.forEach { lesson ->
            assertTrue(lesson.rules.size >= 2)
            assertTrue(lesson.exercises.size >= 4)
            lesson.rules.forEach { rule ->
                assertTrue(rule.titleId.isNotBlank())
                assertTrue(rule.titleEn.isNotBlank())
                assertTrue(rule.exampleAr.isNotBlank())
                assertTrue(rule.exampleEn.isNotBlank())
            }
        }
    }
}
