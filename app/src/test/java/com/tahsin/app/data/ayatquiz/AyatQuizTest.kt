package com.tahsin.app.data.ayatquiz

import com.tahsin.app.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Tes engine "Lengkapi Ayat" (murni). */
class AyatQuizTest {

    private val words = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَٰنِ", "الرَّحِيمِ")
    private val pool = listOf("اللَّهِ", "الرَّحْمَٰنِ", "الرَّحِيمِ", "الْحَمْدُ", "رَبِّ", "الْعَالَمِينَ")

    @Test
    fun `makeQuestion - 4 opsi, jawaban benar ada di dalamnya, tengah dikosongkan`() {
        val q = AyatQuiz.makeQuestion(
            surahNumber = 1,
            ayahNumber = 1,
            words = words,
            pool = pool,
            random = Random(7),
        )
        assertNotNull(q)
        q!!
        assertEquals(4, q.options.size)
        assertTrue(q.correctWord in q.options)
        assertTrue(AyatQuiz.BLANK in q.blankedText)
        // Konteks memuat semua kata kecuali target (yang jadi blank).
        val visible = q.blankedText.split(" ").filter { it != AyatQuiz.BLANK }
        assertEquals(words.size - 1, visible.size)
        assertFalse(q.blankedText.startsWith(AyatQuiz.BLANK)) // bukan kata pertama
    }

    @Test
    fun `makeQuestion - ayat terlalu pendek - null`() {
        assertNull(
            AyatQuiz.makeQuestion(1, 1, listOf("أ", "ب"), pool),
        )
    }

    @Test
    fun `makeQuestion - kolam pengecoh kurang - null`() {
        assertNull(
            AyatQuiz.makeQuestion(1, 1, words, pool = listOf("اللَّهِ")),
        )
    }

    @Test
    fun `makeQuestion - pengecoh bukan bentuk normalisasi sama dengan jawaban`() {
        val q = AyatQuiz.makeQuestion(
            surahNumber = 1,
            ayahNumber = 1,
            words = words,
            // "اللّهِ" (tanpa harakat lengkap) = bentuk normalisasi sama dengan "اللَّهِ".
            pool = listOf("اللّهِ", "الرَّحْمَٰنِ", "الرَّحِيمِ", "الْحَمْدُ"),
            random = Random(3),
        )
        assertNotNull(q)
        q!!
        val norm = ArabicNormalizer.normalize(q.correctWord)
        assertEquals(1, q.options.count { ArabicNormalizer.normalize(it) == norm })
    }

    @Test
    fun `isCorrect - sama persis`() {
        val q = AyatQuiz.makeQuestion(1, 1, words, pool, Random(1))!!
        assertTrue(AyatQuiz.isCorrect(q.correctWord, q))
        assertFalse(AyatQuiz.isCorrect(q.options.first { it != q.correctWord }, q))
    }

    @Test
    fun `isCorrect - beda harakat saja - tetap benar (ternormalisasi)`() {
        val q = AyatQuizQuestion(
            surahNumber = 1,
            ayahNumber = 1,
            blankedText = "بِسْمِ … الرَّحْمَٰنِ الرَّحِيمِ",
            correctWord = "اللَّهِ",
            options = listOf("اللَّهِ", "أ", "ب", "ج"),
        )
        assertTrue(AyatQuiz.isCorrect("اللّهِ", q)) // harakat beda, huruf sama
        assertFalse(AyatQuiz.isCorrect("الرَّحِيمِ", q))
    }


    @Test
    fun `model - getter surah dan nomor ayat konsisten`() {
        val q = AyatQuiz.makeQuestion(3, 5, words, pool, Random(1))!!
        assertEquals(3, q.surahNumber)
        assertEquals(5, q.ayahNumber)
    }


    @Test
    fun `makeQuestion - kata kosong dalam kolam dibuang`() {
        val dirtyPool = pool + listOf("   ", "")
        val q = AyatQuiz.makeQuestion(3, 5, words, dirtyPool, Random(1))
        assertNotNull(q)
        assertTrue(q!!.options.none { it.isBlank() })
    }
}
