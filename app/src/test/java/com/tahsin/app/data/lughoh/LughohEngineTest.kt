package com.tahsin.app.data.lughoh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Tes logika latihan (tadribat) — murni JVM. */
class LughohEngineTest {

    private val fillBlank = FillBlankExercise(
        promptId = "Isi",
        promptAr = "مَا ____؟",
        promptLatin = "Mā ____?",
        options = listOf("اسْمُكِ", "بَيْتُكِ", "كِتَابُكِ", "قَلَمُكِ"),
        answer = "اسْمُكِ",
    )

    private val translate = TranslateArIdExercise(
        promptAr = "مَا اسْمُكِ؟",
        promptLatin = "Mā ismuki?",
        options = listOf("Siapa namamu?", "Apa kabarmu?", "Dari mana kamu?", "Di mana rumahmu?"),
        answer = "Siapa namamu?",
    )

    private val rearrange = RearrangeExercise(
        words = listOf(
            WordChip("أَنَا", "anā"),
            WordChip("مِنْ", "min"),
            WordChip("إِنْدُونِيسِيَّا", "Indūnīsiyyā"),
            WordChip("أَيْضًا", "ayḍan"),
        ),
        answer = listOf("أَنَا", "مِنْ", "إِنْدُونِيسِيَّا", "أَيْضًا"),
    )

    @Test
    fun `pilihan - jawaban tepat benar`() {
        assertTrue(LughohEngine.isChoiceCorrect(fillBlank, "اسْمُكِ"))
        assertTrue(LughohEngine.isChoiceCorrect(translate, "Siapa namamu?"))
    }

    @Test
    fun `pilihan - salah satu opsi lain dianggap salah`() {
        assertFalse(LughohEngine.isChoiceCorrect(fillBlank, "بَيْتُكِ"))
        assertFalse(LughohEngine.isChoiceCorrect(translate, "Dari mana kamu?"))
    }

    @Test
    fun `pilihan - perbedaan harakat tidak dianggap salah (normalisasi)`() {
        // Harakat/sukun dibuang saat pembandingan, seperti build_lughoh.py.
        assertTrue(LughohEngine.isChoiceCorrect(fillBlank, "اسمكِ"))
        assertTrue(LughohEngine.isChoiceCorrect(fillBlank, "اسْمُكِ"))
    }

    @Test
    fun `acak - urutan tampilan tidak sama dengan urutan benar dan memuat semua kata`() {
        val random = Random(42)
        repeat(20) {
            val shown = LughohEngine.shuffleRearrange(rearrange, Random(it))
            assertEquals(rearrange.words.size, shown.size)
            assertEquals(rearrange.words.toSet(), shown.toSet())
            assertNotEquals(rearrange.words, shown)
        }
    }

    @Test
    fun `acak - satu kata tidak diacak`() {
        val single = RearrangeExercise(
            words = listOf(WordChip("أَنَا", "anā")),
            answer = listOf("أَنَا"),
        )
        assertEquals(single.words, LughohEngine.shuffleRearrange(single, Random(1)))
    }

    @Test
    fun `susun - urutan benar dianggap benar`() {
        assertTrue(LughohEngine.isRearrangeCorrect(rearrange, rearrange.words))
    }

    @Test
    fun `susun - urutan salah atau panjang berbeda dianggap salah`() {
        val wrongOrder = listOf(
            WordChip("إِنْدُونِيسِيَّا", "Indūnīsiyyā"),
            WordChip("أَنَا", "anā"),
            WordChip("مِنْ", "min"),
            WordChip("أَيْضًا", "ayḍan"),
        )
        assertFalse(LughohEngine.isRearrangeCorrect(rearrange, wrongOrder))
        assertFalse(LughohEngine.isRearrangeCorrect(rearrange, rearrange.words.dropLast(1)))
    }

    @Test
    fun `susun - varian harakat pada kata tetap dianggap benar`() {
        val noMarks = rearrange.words.map { WordChip(it.ar.replace("َ", ""), it.latin) }
        assertTrue(LughohEngine.isRearrangeCorrect(rearrange, noMarks))
    }

    @Test
    fun `susun - pemeriksaan per posisi membedakan kata benar dan salah`() {
        val correct = rearrange.words
        assertTrue(LughohEngine.isChipAtPositionCorrect(rearrange, correct[0], 0))
        assertTrue(LughohEngine.isChipAtPositionCorrect(rearrange, correct[3], 3))
        // Kata benar tetapi di posisi salah.
        assertFalse(LughohEngine.isChipAtPositionCorrect(rearrange, correct[3], 0))
        // Di luar panjang jawaban.
        assertFalse(LughohEngine.isChipAtPositionCorrect(rearrange, correct[0], 99))
    }

    // ---- Sesi acak (arcade) ----

    private fun lessonOf(id: String, vararg exercises: Exercise) = LughohLesson(
        id = id,
        titleId = id,
        titleAr = id,
        muhadatsah = emptyList(),
        mufrodat = emptyList(),
        qawaid = emptyList(),
        tadribat = exercises.toList(),
    )

    private fun answerOf(ex: Exercise): String = when (ex) {
        is FillBlankExercise -> ex.answer
        is TranslateArIdExercise -> ex.answer
        is TranslateIdArExercise -> ex.answer
        is RearrangeExercise -> ex.answer.joinToString(" ")
    }

    @Test
    fun `sesi acak - jumlah sesuai, unik, jawaban berasal dari seluruh pelajaran`() {
        val lessons = (1..5).map { n -> lessonOf("L$n", fillBlank, translate) }
        val session = LughohEngine.buildRandomSession(lessons, 8, Random(3))
        assertEquals(8, session.size)
        assertEquals(8, session.distinct().size)
        val poolAnswers = lessons.flatMap { it.tadribat }.map { answerOf(it) }
        // Opsi diacak → latihan sesi adalah salinan, jadi cocokkan lewat jawaban.
        assertTrue(session.all { answerOf(it) in poolAnswers })
    }

    @Test
    fun `sesi acak - count melebihi kolam berarti ambil semua, pelajaran kosong berarti kosong`() {
        val lessons = listOf(lessonOf("L1", fillBlank, translate, rearrange))
        assertEquals(3, LughohEngine.buildRandomSession(lessons, 99, Random(1)).size)
        assertEquals(0, LughohEngine.buildRandomSession(emptyList(), 8, Random(1)).size)
    }

    @Test
    fun `sesi acak - deterministik per random (seed sama hasil sama)`() {
        val lessons = (1..5).map { n -> lessonOf("L$n", fillBlank, translate, rearrange) }
        val a = LughohEngine.buildRandomSession(lessons, 8, Random(42))
        val b = LughohEngine.buildRandomSession(lessons, 8, Random(42))
        assertEquals(a, b)
    }

    @Test
    fun `acak opsi - jawaban tetap, isi opsi sama, urutan bisa berubah`() {
        val shuffled = LughohEngine.shuffleOptions(fillBlank, Random(9)) as FillBlankExercise
        assertEquals(fillBlank.answer, shuffled.answer)
        assertEquals(fillBlank.options.toSet(), shuffled.options.toSet())
        assertTrue(
            fillBlank.options != shuffled.options ||
                fillBlank.options.zip(shuffled.options).any { it.first != it.second },
        )
    }

    @Test
    fun `acak opsi - latihan menyusun kata tidak diubah`() {
        assertEquals(rearrange, LughohEngine.shuffleOptions(rearrange, Random(5)))
    }
}
