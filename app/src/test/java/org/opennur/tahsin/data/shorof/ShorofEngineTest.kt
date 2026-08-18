package org.opennur.tahsin.data.shorof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShorofEngineTest {
    private val exercise = ShorofChoiceExercise(
        "Pilih", "Choose", "كَتَبَ", "kataba", listOf("A", "B", "C"), listOf("A", "B", "C"), 1,
    )

    private fun lesson(id: String, vararg exercises: ShorofExercise) = ShorofLesson(
        id, id, id, id, id, id, emptyList(), emptyList(), emptyList(), exercises.toList(),
    )

    @Test
    fun `jawaban pilihan benar dan salah`() {
        assertTrue(ShorofEngine.isChoiceCorrect(exercise, 1))
        assertFalse(ShorofEngine.isChoiceCorrect(exercise, 0))
    }

    @Test
    fun `sesi mengambil soal dari semua lesson`() {
        val lessons = (1..5).map { lesson("$it", exercise, exercise.copy(answerIndex = 0)) }
        val session = ShorofEngine.buildSession(lessons, 8, Random(1))
        assertEquals(8, session.size)
        assertTrue(session.all { it.lessonId in lessons.map(ShorofLesson::id) })
        assertTrue(ShorofEngine.buildSession(emptyList(), 8).isEmpty())
        assertEquals(2, ShorofEngine.buildSession(listOf(lesson("one", exercise, exercise)), 99).size)
    }
}
