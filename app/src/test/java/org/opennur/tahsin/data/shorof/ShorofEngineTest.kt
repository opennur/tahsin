package org.opennur.tahsin.data.shorof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun `pilihan jawaban batas indeks`() {
        val first = ShorofChoiceExercise("a", "a", "a", "a", listOf("X", "Y", "Z"), listOf("X", "Y", "Z"), 0)
        val last = ShorofChoiceExercise("b", "b", "b", "b", listOf("X", "Y", "Z"), listOf("X", "Y", "Z"), 2)
        assertTrue(ShorofEngine.isChoiceCorrect(first, 0))
        assertTrue(ShorofEngine.isChoiceCorrect(last, 2))
        assertFalse(ShorofEngine.isChoiceCorrect(first, 2))
        assertFalse(ShorofEngine.isChoiceCorrect(last, 0))
    }

    @Test
    fun `sesi mengambil soal dari semua lesson`() {
        val lessons = (1..5).map { lesson("$it", exercise, exercise.copy(answerIndex = 0)) }
        val session = ShorofEngine.buildSession(lessons, 8, Random(1))
        assertEquals(8, session.size)
        assertTrue(session.all { it.lessonId in lessons.map(ShorofLesson::id) })
        assertTrue(ShorofEngine.buildSession(emptyList(), 8).isEmpty())
        assertEquals(2, ShorofEngine.buildSession(listOf(lesson("one", exercise, exercise)), 99).size)
        val one = lesson("one", exercise)
        assertEquals(listOf("one:0"), ShorofEngine.allQuestionIds(listOf(one)))
        assertEquals(1, ShorofEngine.buildSession(listOf(one), 8, Random(1), setOf("one:0")).size)
        assertTrue(ShorofEngine.buildSession(listOf(one), 8, Random(1), setOf("missing")).isEmpty())
    }

    @Test
    fun `sesi lebih banyak pertanyaan daripada latihan`() {
        val lessons = listOf(lesson("1", exercise))
        val session = ShorofEngine.buildSession(lessons, 100, Random(42))
        assertEquals(1, session.size)
    }

    @Test
    fun `sesi count nol menghasilkan kosong`() {
        val lessons = listOf(lesson("1", exercise))
        assertTrue(ShorofEngine.buildSession(lessons, 0).isEmpty())
    }

    @Test
    fun `sesi dengan banyak latihan per lesson`() {
        val exercises = (1..20).map { i ->
            ShorofChoiceExercise("$i", "$i", "أ", "a", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val lessons = listOf(lesson("1", *exercises.toTypedArray()))
        val session = ShorofEngine.buildSession(lessons, 8, Random(42))
        assertEquals(8, session.size)
    }

    @Test
    fun `allowedIds memfilter sesi`() {
        val exercises = (1..5).map { i ->
            ShorofChoiceExercise("$i", "$i", "أ", "a", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val lessons = listOf(lesson("1", *exercises.toTypedArray()))
        val allIds = ShorofEngine.allQuestionIds(lessons)
        assertEquals(5, allIds.size)
        val filtered = ShorofEngine.buildSession(lessons, 10, Random(42), setOf("1:2", "1:4"))
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.questionId in setOf("1:2", "1:4") })
    }

    @Test
    fun `allowedIds kosong menghasilkan sesi kosong`() {
        val lessons = listOf(lesson("1", exercise))
        val session = ShorofEngine.buildSession(lessons, 8, Random(42), emptySet())
        assertEquals(0, session.size)
    }

    @Test
    fun `allQuestionIds dengan lesson kosong`() {
        assertEquals(emptyList<String>(), ShorofEngine.allQuestionIds(emptyList()))
    }

    @Test
    fun `allQuestionIds urut berdasarkan lesson dan indeks`() {
        val l1 = lesson("1-1", exercise, exercise, exercise)
        val l2 = lesson("1-2", exercise)
        val ids = ShorofEngine.allQuestionIds(listOf(l1, l2))
        assertEquals(listOf("1-1:0", "1-1:1", "1-1:2", "1-2:0"), ids)
    }

    @Test
    fun `questionId format benar`() {
        assertEquals("abc:5", ShorofEngine.questionId("abc", 5))
        assertEquals("1-3:0", ShorofEngine.questionId("1-3", 0))
    }

    @Test
    fun `exerciseQuestionId unik`() {
        val exercises = (0..9).map { i ->
            ShorofChoiceExercise("$i", "$i", "أ", "a", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val l = lesson("1-1", *exercises.toTypedArray())
        val ids = ShorofEngine.allQuestionIds(listOf(l))
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `sesi deterministik dengan random seed sama`() {
        val lessons = (1..3).map { lesson("$it", exercise) }
        val s1 = ShorofEngine.buildSession(lessons, 6, Random(99))
        val s2 = ShorofEngine.buildSession(lessons, 6, Random(99))
        assertEquals(s1.map { it.questionId }, s2.map { it.questionId })
    }

    @Test
    fun `sesi tidak mengulang pertanyaan dalam satu sesi`() {
        val exercises = (1..20).map { i ->
            ShorofChoiceExercise("$i", "$i", "أ", "a", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val lessons = listOf(lesson("1", *exercises.toTypedArray()))
        val session = ShorofEngine.buildSession(lessons, 10, Random(42))
        val ids = session.map { it.questionId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `session exercise menyimpan lessonId dan questionId`() {
        val one = lesson("2-3", exercise)
        val session = ShorofEngine.buildSession(listOf(one), 1, Random(1))
        assertEquals(1, session.size)
        assertEquals("2-3", session[0].lessonId)
        assertEquals("2-3:0", session[0].questionId)
    }
}
