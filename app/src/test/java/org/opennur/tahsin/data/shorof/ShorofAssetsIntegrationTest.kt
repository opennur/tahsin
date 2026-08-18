@file:Suppress("MaxLineLength")

package org.opennur.tahsin.data.shorof

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShorofAssetsIntegrationTest {
    private val root: File by lazy {
        listOf(File("src/main/assets"), File("app/src/main/assets"), File(System.getProperty("user.dir"), "app/src/main/assets"))
            .firstOrNull { File(it, "shorof/lessons.json").isFile } ?: error("aset Shorof tidak ditemukan")
    }

    @Test
    fun `aset Shorof memiliki delapan lesson dan data lengkap`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        assertEquals(1, catalog.schemaVersion)
        assertEquals(3, catalog.levels.size)
        assertEquals(8, catalog.levels.sumOf { it.lessons.size })
        catalog.levels.flatMap { it.lessons }.forEach { lesson ->
            assertTrue(lesson.rules.size >= 2)
            assertTrue(lesson.exercises.size >= 4)
            lesson.patterns.forEach { assertTrue(it.root.isNotBlank() && it.wazan.isNotBlank()) }
        }
    }
}
