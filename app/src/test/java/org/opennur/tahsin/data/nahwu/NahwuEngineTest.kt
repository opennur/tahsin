package org.opennur.tahsin.data.nahwu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NahwuEngineTest {
    private val choice = NahwuChoiceExercise(
        promptId = "Pilih",
        promptEn = "Choose",
        promptAr = "هٰذَا",
        promptLatin = "Hādhā",
        optionsId = listOf("Isim", "Fi'il", "Harf"),
        optionsEn = listOf("Noun", "Verb", "Particle"),
        answerIndex = 0,
    )
    private val rearrange = NahwuRearrangeExercise(
        promptId = "Susun",
        promptEn = "Arrange",
        words = listOf(NahwuWord("أَنَا", "anā"), NahwuWord("طَالِبٌ", "ṭālibun")),
    )

    private fun lesson(id: String, vararg exercises: NahwuExercise) = NahwuLesson(
        id = id,
        titleId = id,
        titleEn = id,
        titleAr = id,
        introId = id,
        introEn = id,
        rules = emptyList(),
        exercises = exercises.toList(),
    )

    @Test
    fun `pilihan benar dan salah`() {
        assertTrue(NahwuEngine.isChoiceCorrect(choice, 0))
        assertFalse(NahwuEngine.isChoiceCorrect(choice, 2))
    }

    @Test
    fun `sesi acak mengambil dari semua lesson`() {
        val lessons = (1..5).map { lesson("$it", choice, rearrange) }
        val session = NahwuEngine.buildSession(lessons, 8, Random(42))
        assertEquals(8, session.size)
        assertTrue(session.all { it.lessonId in lessons.map(NahwuLesson::id) })
        assertEquals(0, NahwuEngine.buildSession(emptyList(), 8).size)
        assertEquals(2, NahwuEngine.buildSession(listOf(lesson("one", choice, rearrange)), 99).size)
        val one = lesson("one", choice, rearrange)
        assertEquals(listOf("one:0", "one:1"), NahwuEngine.allQuestionIds(listOf(one)))
        assertEquals(1, NahwuEngine.buildSession(listOf(one), 8, Random(1), setOf("one:1")).size)
    }

    @Test
    fun `acak kata mempertahankan isi dan mengubah urutan`() {
        val shown = NahwuEngine.shuffleWords(rearrange, Random(42))
        assertEquals(rearrange.words.toSet(), shown.toSet())
        assertNotEquals(rearrange.words, shown)
        assertEquals(rearrange.words.size, NahwuEngine.shuffleWords(rearrange).size)
        repeat(100) { NahwuEngine.shuffleWords(rearrange, Random(it)) }
        val duplicateWords = rearrange.copy(words = listOf(rearrange.words[0], rearrange.words[0]))
        assertEquals(duplicateWords.words, NahwuEngine.shuffleWords(duplicateWords, Random(1)))
        val single = rearrange.copy(words = listOf(rearrange.words.first()))
        assertEquals(single.words, NahwuEngine.shuffleWords(single, Random(1)))
    }

    @Test
    fun `susun memvalidasi urutan dan panjang`() {
        val shown = listOf(rearrange.words[1], rearrange.words[0])
        assertTrue(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(1, 0)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(0, 1)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(1)))
        val noMarks = rearrange.words.map { it.copy(ar = it.ar.replace("َ", "")) }
        assertTrue(NahwuEngine.isRearrangeCorrect(rearrange, noMarks, listOf(0, 1)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, emptyList(), listOf(0, 1)))
    }
}
