package org.opennur.tahsin.data.lughoh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gap-coverage: engine & model Belajar Arab (panggilan default-arg + pembaca properti). */
class LughohEngineGapsTest {

    private fun exerciseFill(answer: String) = FillBlankExercise(
        promptId = "P1", promptAr = "أنا ____ طالب", promptLatin = "ana ____ thālib",
        options = listOf(answer, "خاطئ"), answer = answer,
    )

    private fun lesson(exercises: List<Exercise> = listOf(exerciseFill("طالب"))) = LughohLesson(
        id = "l1",
        titleId = "Perkenalan",
        titleAr = "التعارف",
        muhadatsah = listOf(DialogueLine("A", "مرحبا", "marhaban", "Halo")),
        mufrodat = listOf(
            VocabWord("الكتاب", "al-kitāb", "buku", "contoh ar", "contoh latin", "contoh id"),
        ),
        qawaid = listOf(GrammarRule("ال", "al", "contoh ar", "contoh latin", "contoh id")),
        tadribat = exercises,
    )

    private fun rearrange() = RearrangeExercise(
        words = listOf(WordChip("أنا", "ana"), WordChip("طالب", "thālib")),
        answer = listOf("أنا", "طالب"),
    )

    // ---- buildRandomSession (default random → $default; koleksi kosong) ----

    @Test
    fun `buildRandomSession - tanpa random eksplisit - tetap jalan`() {
        val session = LughohEngine.buildRandomSession(listOf(lesson()), count = 3)
        assertEquals(1, session.size)
    }

    @Test
    fun `buildRandomSession - tanpa latihan - kosong`() {
        assertTrue(LughohEngine.buildRandomSession(listOf(lesson(emptyList())), 5).isEmpty())
        assertTrue(LughohEngine.buildRandomSession(emptyList(), 5).isEmpty())
    }

    // ---- isChoiceCorrect ----

    @Test
    fun `isChoiceCorrect - benar & salah untuk tiap tipe`() {
        val fill = exerciseFill("طالب")
        assertTrue(LughohEngine.isChoiceCorrect(fill, "طالب"))
        assertFalse(LughohEngine.isChoiceCorrect(fill, "خاطئ"))

        val arId = TranslateArIdExercise("أنا طالب", "ana thālib", listOf("Saya pelajar", "Saya guru"), "Saya pelajar")
        assertTrue(LughohEngine.isChoiceCorrect(arId, "Saya pelajar")) // normalisasi teks Arab
        assertFalse(LughohEngine.isChoiceCorrect(arId, "Saya guru"))

        val idAr = TranslateIdArExercise("Saya pelajar", listOf("أنا طالب", "أنا معلم"), "أنا طالب")
        assertTrue(LughohEngine.isChoiceCorrect(idAr, "أنا طالب"))
        assertFalse(LughohEngine.isChoiceCorrect(idAr, "أنا معلم"))
    }

    @Test
    fun `isChoiceCorrect - latihan menyusun kata selalu salah (bukan pilihan)`() {
        assertFalse(LughohEngine.isChoiceCorrect(rearrange(), "أنا طالب"))
    }

    // ---- pembaca properti model (getter yang jarang dibaca) ----

    @Test
    fun `model - getter yang jarang dibaca tetap konsisten`() {
        val l = lesson()
        val fill = exerciseFill("طالب")
        val arId = TranslateArIdExercise("أنا طالب", "ana thālib", listOf("Saya pelajar"), "Saya pelajar")
        val idAr = TranslateIdArExercise("Saya pelajar", listOf("أنا طالب"), "أنا طالب")
        val word = l.mufrodat[0]
        val grammar = l.qawaid[0]

        assertEquals("Perkenalan", l.titleId)
        assertEquals("التعارف", l.titleAr)
        assertEquals("الكتاب", word.ar)
        assertEquals("contoh latin", word.exampleLatin)
        assertEquals("contoh id", word.exampleId)
        assertEquals("contoh latin", grammar.exampleLatin)
        assertEquals("contoh id", grammar.exampleId)
        assertEquals("ال", grammar.titleId)
        assertEquals("P1", fill.promptId)
        assertEquals("أنا ____ طالب", fill.promptAr)
        assertEquals("ana ____ thālib", fill.promptLatin)
        assertEquals("أنا ⋯⋯ طالب", fill.displayPromptAr)
        assertEquals("ana ⋯⋯ thālib", fill.displayPromptLatin)
        assertEquals(listOf("أنا طالب"), idAr.options)
        assertEquals("أنا طالب", idAr.answer)
        assertEquals("أنا طالب", arId.promptAr)
        assertEquals("ana thālib", arId.promptLatin)
        assertEquals("طالب", fill.answer)
        assertEquals(ExerciseType.FILL_BLANK, fill.type)
        assertEquals(ExerciseType.TRANSLATE_AR_ID, arId.type)
        assertEquals(ExerciseType.TRANSLATE_ID_AR, idAr.type)
        assertEquals(ExerciseType.REARRANGE, rearrange().type)
        assertEquals(4, ExerciseType.entries.size)
    }


    @Test
    fun `shuffleOptions - tiap tipe pilihan diacak`() {
        val r = kotlin.random.Random(7)
        val fill = FillBlankExercise("P", "أ ____", "a ____", listOf("أ", "ب"), "أ")
        val arId = TranslateArIdExercise("أ", "a", listOf("x", "y"), "x")
        val idAr = TranslateIdArExercise("x", listOf("أ", "ب"), "أ")
        assertEquals(2, (LughohEngine.shuffleOptions(fill, r) as FillBlankExercise).options.size)
        assertEquals(2, (LughohEngine.shuffleOptions(arId, r) as TranslateArIdExercise).options.size)
        assertEquals(2, (LughohEngine.shuffleOptions(idAr, r) as TranslateIdArExercise).options.size)
    }

    @Test
    fun `shuffleRearrange - random macet - guard-loop berhenti`() {
        // Random yang selalu mengembalikan urutan asli → loop guard berhenti
        // setelah 10 percobaan dan tetap mengembalikan urutan asli (aman).
        // Random "macet": nextInt(bound) selalu mengembalikan bound-1 →
        // shuffle identitas → guard-loop berhenti setelah 10 percobaan.
        val stuck = object : kotlin.random.Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(until: Int): Int = until - 1
        }
        val chips = listOf(WordChip("أ", "a"), WordChip("ب", "b"), WordChip("ج", "c"))
        val ex = RearrangeExercise(words = chips, answer = listOf("أ", "ب", "ج"))
        val out = LughohEngine.shuffleRearrange(ex, stuck)
        assertEquals(chips, out)
    }
}
