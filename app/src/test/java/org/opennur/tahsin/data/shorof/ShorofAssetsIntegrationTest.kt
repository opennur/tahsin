@file:Suppress("MaxLineLength")

package org.opennur.tahsin.data.shorof

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `semua lesson memiliki id unik`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        val ids = catalog.levels.flatMap { it.lessons }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `semua latihan choice memiliki minimal tiga opsi`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<ShorofChoiceExercise>().forEach { exercise ->
                assertTrue(exercise.optionsId.size >= 3)
                assertEquals(exercise.optionsId.size, exercise.optionsEn.size)
                assertTrue(exercise.answerIndex in exercise.optionsId.indices)
                assertTrue(exercise.promptAr.isNotBlank())
                assertTrue(exercise.promptLatin.isNotBlank())
            }
    }

    @Test
    fun `latihan choice bisa dievaluasi dengan isChoiceCorrect`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<ShorofChoiceExercise>().forEach { exercise ->
                assertTrue(ShorofEngine.isChoiceCorrect(exercise, exercise.answerIndex))
                if (exercise.answerIndex > 0) {
                    assertFalse(ShorofEngine.isChoiceCorrect(exercise, 0))
                } else if (exercise.optionsId.size > 1) {
                    assertFalse(ShorofEngine.isChoiceCorrect(exercise, 1))
                }
            }
    }

    @Test
    fun `semua pattern memiliki root dan wazan`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.patterns }.forEach { pattern ->
            assertTrue(pattern.root.isNotBlank())
            assertTrue(pattern.wazan.isNotBlank())
            assertTrue(pattern.rootLatin.isNotBlank())
            assertTrue(pattern.wazanLatin.isNotBlank())
            assertTrue(pattern.formId.isNotBlank())
            assertTrue(pattern.meaningId.isNotBlank())
            assertTrue(pattern.exampleAr.isNotBlank())
            assertTrue(pattern.exampleLatin.isNotBlank())
        }
    }

    @Test
    fun `conjugations memiliki data lengkap`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.conjugations }.forEach { conj ->
            assertTrue(conj.pronounAr.isNotBlank())
            assertTrue(conj.pronounLatin.isNotBlank())
            assertTrue(conj.past.isNotBlank())
            assertTrue(conj.present.isNotBlank())
            assertTrue(conj.imperative.isNotBlank())
        }
    }

    @Test
    fun `setiap level memiliki id urut dan judul lengkap`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.forEachIndexed { index, level ->
            assertEquals(index + 1, level.id)
            assertTrue(level.titleId.isNotBlank())
            assertTrue(level.titleEn.isNotBlank())
            assertTrue(level.titleAr.isNotBlank())
        }
    }

    @Test
    fun `semua rule memiliki contoh arab dan latin`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.rules }.forEach { rule ->
            assertTrue(rule.exampleAr.isNotBlank())
            assertTrue(rule.exampleLatin.isNotBlank())
            assertTrue(rule.explanationId.isNotBlank())
            assertTrue(rule.explanationEn.isNotBlank())
        }
    }

    @Test
    fun `allQuestionIds menghasilkan jumlah yang konsisten`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        val lessons = catalog.levels.flatMap { it.lessons }
        val ids = ShorofEngine.allQuestionIds(lessons)
        assertEquals(lessons.sumOf { it.exercises.size }, ids.size)
    }

    @Test
    fun `lesson memiliki introId dan introEn tidak kosong`() {
        val catalog = ShorofParser.parse(File(root, "shorof/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.forEach { lesson ->
            assertTrue(lesson.introId.isNotBlank())
            assertTrue(lesson.introEn.isNotBlank())
        }
    }
}
