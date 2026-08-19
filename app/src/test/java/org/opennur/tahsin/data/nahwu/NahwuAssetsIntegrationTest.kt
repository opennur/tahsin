package org.opennur.tahsin.data.nahwu

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `aset asli memiliki tiga belas pelajaran dan latihan lengkap`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        assertEquals(1, catalog.schemaVersion)
        assertEquals(3, catalog.levels.size)
        assertEquals(13, catalog.levels.sumOf { it.lessons.size })
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

    @Test
    fun `semua lesson memiliki id unik`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        val ids = catalog.levels.flatMap { it.lessons }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `semua latihan memiliki promptId dan promptEn tidak kosong`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }.forEach { exercise ->
            assertTrue(exercise.promptId.isNotBlank())
            assertTrue(exercise.promptEn.isNotBlank())
        }
    }

    @Test
    fun `latihan choice memiliki minimal tiga opsi dan indeks valid`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<NahwuChoiceExercise>().forEach { exercise ->
                assertTrue(exercise.optionsId.size >= 3)
                assertEquals(exercise.optionsId.size, exercise.optionsEn.size)
                assertTrue(exercise.answerIndex in exercise.optionsId.indices)
                assertTrue(exercise.promptAr.isNotBlank())
                assertTrue(exercise.promptLatin.isNotBlank())
            }
    }

    @Test
    fun `latihan rearrange memiliki minimal dua kata`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<NahwuRearrangeExercise>().forEach { exercise ->
                assertTrue(exercise.words.size >= 2)
                exercise.words.forEach { word ->
                    assertTrue(word.ar.isNotBlank())
                    assertTrue(word.latin.isNotBlank())
                }
            }
    }

    @Test
    fun `latihan rearrange bisa diselesaikan dengan shuffleWords`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<NahwuRearrangeExercise>().forEach { exercise ->
                val shuffled = NahwuEngine.shuffleWords(exercise, Random(42))
                assertEquals(exercise.words.size, shuffled.size)
                assertEquals(exercise.words.toSet(), shuffled.toSet())
            }
    }

    @Test
    fun `latihan choice bisa dievaluasi dengan isChoiceCorrect`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<NahwuChoiceExercise>().forEach { exercise ->
                assertTrue(NahwuEngine.isChoiceCorrect(exercise, exercise.answerIndex))
                if (exercise.answerIndex > 0) {
                    assertFalse(NahwuEngine.isChoiceCorrect(exercise, exercise.answerIndex - 1))
                }
            }
    }

    @Test
    fun `latihan rearrange bisa dievaluasi dengan isRearrangeCorrect`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<NahwuRearrangeExercise>().forEach { exercise ->
                assertTrue(NahwuEngine.isRearrangeCorrect(exercise, exercise.words, exercise.words.indices.toList()))
                if (exercise.words.size >= 2) {
                    val reversed = exercise.words.reversed()
                    val reversedIndices = exercise.words.indices.reversed().toList()
                    if (exercise.words.toSet().size == exercise.words.size) {
                        assertTrue(NahwuEngine.isRearrangeCorrect(exercise, reversed, reversedIndices))
                    }
                }
            }
    }

    @Test
    fun `setiap level memiliki id urut dan judul lengkap`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.forEachIndexed { index, level ->
            assertEquals(index + 1, level.id)
            assertTrue(level.titleId.isNotBlank())
            assertTrue(level.titleEn.isNotBlank())
            assertTrue(level.titleAr.isNotBlank())
        }
    }

    @Test
    fun `semua rule memiliki contoh arab dan latin`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        catalog.levels.flatMap { it.lessons }.flatMap { it.rules }.forEach { rule ->
            assertTrue(rule.exampleAr.isNotBlank())
            assertTrue(rule.exampleLatin.isNotBlank())
            assertTrue(rule.explanationId.isNotBlank())
            assertTrue(rule.explanationEn.isNotBlank())
        }
    }

    @Test
    fun `allQuestionIds menghasilkan jumlah yang konsisten dengan jumlah exercise`() {
        val catalog = NahwuParser.parse(File(assetsRoot, "nahwu/lessons.json").readText())
        val lessons = catalog.levels.flatMap { it.lessons }
        val ids = NahwuEngine.allQuestionIds(lessons)
        assertEquals(lessons.sumOf { it.exercises.size }, ids.size)
    }
}
