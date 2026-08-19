package org.opennur.tahsin.data.shorof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShorofConjugationExerciseTest {
    private val exercise = ShorofConjugationExercise(
        promptId = "Lengkapi bentuk Madhi untuk هُوَ",
        promptEn = "Complete the past tense form for huwa",
        promptAr = "هُوَ",
        promptLatin = "huwa",
        optionsId = listOf("كَتَبَ", "يَكْتُبُ", "اُكْتُبْ", "كَتَبُوا"),
        optionsEn = listOf("كَتَبَ", "يَكْتُبُ", "اُكْتُبْ", "كَتَبُوا"),
        answerIndex = 0,
    )

    private fun lesson(id: String, vararg exercises: ShorofExercise) = ShorofLesson(
        id, id, id, id, id, id, emptyList(), emptyList(), emptyList(), exercises.toList(),
    )

    @Test
    fun `isConjugationCorrect benar dan salah`() {
        assertTrue(ShorofEngine.isConjugationCorrect(exercise, 0))
        assertFalse(ShorofEngine.isConjugationCorrect(exercise, 1))
        assertFalse(ShorofEngine.isConjugationCorrect(exercise, 2))
    }

    @Test
    fun `conjugationAnswer mengembalikan jawaban benar`() {
        assertEquals("كَتَبَ", ShorofEngine.conjugationAnswer(exercise))
        assertEquals("Lengkapi bentuk Madhi untuk هُوَ", exercise.promptId)
        assertEquals("Complete the past tense form for huwa", exercise.promptEn)
        assertEquals("huwa", exercise.promptLatin)
        assertEquals(exercise.optionsId, exercise.optionsEn)
    }

    @Test
    fun `conjugation exercise bisa disertakan dalam sesi`() {
        val exercises: List<ShorofExercise> = listOf(exercise, exercise.copy(answerIndex = 1))
        val l = lesson("1-1", *exercises.toTypedArray())
        val session = ShorofEngine.buildSession(listOf(l), 2, Random(42))
        assertEquals(2, session.size)
        assertTrue(session.all { it.exercise is ShorofConjugationExercise })
    }

    @Test
    fun `conjugation dan choice exercise bisa campur dalam sesi`() {
        val choice = ShorofChoiceExercise(
            "Pilih", "Choose", "كَتَبَ", "kataba",
            listOf("A", "B", "C"), listOf("A", "B", "C"), 0,
        )
        val l = lesson("1-1", exercise, choice)
        val session = ShorofEngine.buildSession(listOf(l), 2, Random(42))
        assertEquals(2, session.size)
        val types = session.map { it.exercise::class }
        assertTrue(types.contains(ShorofConjugationExercise::class))
        assertTrue(types.contains(ShorofChoiceExercise::class))
    }

    @Test
    fun `parser membaca conjugation type`() {
        val json = """
            {"schemaVersion":1,"levels":[{"id":1,"lessons":[
              {"id":"1-1","titleId":"A","titleEn":"A","titleAr":"أ",
               "introId":"i","introEn":"i",
               "rules":[],"patterns":[],"conjugations":[],
               "exercises":[
                 {"type":"conjugation","promptId":"P","promptEn":"P",
                  "promptAr":"هُوَ","promptLatin":"huwa",
                  "optionsId":["A","B","C"],"optionsEn":["A","B","C"],"answerIndex":0}
               ]}
            ]}]}
        """.trimIndent()
        val catalog = ShorofParser.parse(json)
        val ex = catalog.levels[0].lessons[0].exercises[0]
        assertTrue(ex is ShorofConjugationExercise)
        val conj = ex as ShorofConjugationExercise
        assertEquals("هُوَ", conj.promptAr)
        assertEquals(0, conj.answerIndex)
    }

    @Test
    fun `conjugation exercise dengan jawaban indeks berbeda`() {
        val ex = ShorofConjugationExercise(
            "a", "a", "هُوَ", "huwa",
            listOf("كَتَبَ", "يَكْتُبُ", "اُكْتُبْ"),
            listOf("kataba", "yaktubu", "uktub"),
            2,
        )
        assertFalse(ShorofEngine.isConjugationCorrect(ex, 0))
        assertFalse(ShorofEngine.isConjugationCorrect(ex, 1))
        assertTrue(ShorofEngine.isConjugationCorrect(ex, 2))
        assertEquals("اُكْتُبْ", ShorofEngine.conjugationAnswer(ex))
    }

    @Test
    fun `conjugation exercise dari aset bisa dievaluasi`() {
        val root = java.io.File("app/src/main/assets/shorof/lessons.json")
        if (!root.exists()) return
        val catalog = ShorofParser.parse(root.readText())
        val conjugations = catalog.levels.flatMap { it.lessons }.flatMap { it.exercises }
            .filterIsInstance<ShorofConjugationExercise>()
        assertTrue(conjugations.isNotEmpty())
        conjugations.forEach { ex ->
            assertTrue(ShorofEngine.isConjugationCorrect(ex, ex.answerIndex))
            if (ex.answerIndex > 0) {
                assertFalse(ShorofEngine.isConjugationCorrect(ex, 0))
            } else if (ex.optionsId.size > 1) {
                assertFalse(ShorofEngine.isConjugationCorrect(ex, 1))
            }
        }
    }
}
