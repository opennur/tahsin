@file:Suppress("MaxLineLength")

package org.opennur.tahsin.data.nahwu

import org.junit.Assert.assertEquals
import org.junit.Test

class NahwuModelsTest {
    @Test
    fun `semua model mengekspos data lengkap`() {
        val word = NahwuWord("أَنَا", "anā")
        val choice = NahwuChoiceExercise(
            "Pilih",
            "Choose",
            "أَنَا",
            "anā",
            listOf("A"),
            listOf("A"),
            0,
        )
        val rearrange = NahwuRearrangeExercise("Susun", "Arrange", listOf(word))
        val rule = NahwuRule("Judul", "Title", "Penjelasan", "Explanation", "أَنَا", "anā", "Saya", "I")
        val lesson = NahwuLesson(
            "1-1",
            "Pelajaran",
            "Lesson",
            "الدَّرْسُ",
            "Pengantar",
            "Intro",
            listOf(rule),
            listOf(choice, rearrange),
        )
        val level = NahwuLevel(1, "Dasar", "Basics", "الْأَسَاسُ", listOf(lesson))
        val catalog = NahwuCatalog(1, listOf(level))
        val sessionChoice = NahwuSessionExercise("1-1", choice)
        val sessionRearrange = NahwuSessionExercise("1-1", rearrange)

        assertEquals(1, catalog.schemaVersion)
        assertEquals(1, catalog.levels[0].id)
        assertEquals("Dasar", level.titleId)
        assertEquals("الْأَسَاسُ", level.titleAr)
        assertEquals("Pelajaran", lesson.titleId)
        assertEquals("Lesson", lesson.titleEn)
        assertEquals("الدَّرْسُ", lesson.titleAr)
        assertEquals("Pengantar", lesson.introId)
        assertEquals("Penjelasan", rule.explanationId)
        assertEquals("Explanation", rule.explanationEn)
        assertEquals("anā", rule.exampleLatin)
        assertEquals("Saya", rule.exampleId)
        assertEquals("anā", word.latin)
        assertEquals("Pilih", choice.promptId)
        assertEquals("Choose", choice.promptEn)
        assertEquals("أَنَا", choice.promptAr)
        assertEquals("anā", choice.promptLatin)
        assertEquals(listOf("A"), choice.optionsId)
        assertEquals(0, choice.answerIndex)
        assertEquals("Susun", rearrange.promptId)
        assertEquals("Arrange", rearrange.promptEn)
        assertEquals(listOf(word), rearrange.words)
        assertEquals(choice, sessionChoice.exercise)
        assertEquals(rearrange, sessionRearrange.exercise)
    }
}
