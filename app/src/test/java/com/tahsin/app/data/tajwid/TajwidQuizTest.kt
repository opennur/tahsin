package com.tahsin.app.data.tajwid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tes generator kuis tajwid: pemilihan kata ber-hukum, opsi pilihan ganda,
 * dan penilaian jawaban (deterministik dengan seed).
 */
class TajwidQuizTest {

    @Test
    fun `pickWord memilih kata yang punya hukum dan menyertakan jawaban benar`() {
        val words = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَٰنِ", "الرَّحِيمِ")
        val q = TajwidQuiz.pickWord(words, Random(1)) ?: throw AssertionError("harus ada soal")
        assertEquals(4, q.options.size)
        assertTrue("opsi harus memuat jawaban benar", q.options.contains(q.targetRule.name))
        // Kata yang dipilih harus punya hukum itu.
        val rules = TajwidEngine.analyzeWord(q.word, q.prevWord, q.nextWord)
        assertTrue(rules.any { it.name == q.targetRule.name })
    }

    @Test
    fun `pickWord null kalau tidak ada kata ber-hukum`() {
        // مِنْ (nun sukun tanpa konteks) tidak punya hukum → null
        assertTrue(TajwidQuiz.pickWord(listOf("مِنْ"), Random(0)) == null)
        assertTrue(TajwidQuiz.pickWord(emptyList(), Random(0)) == null)
    }

    @Test
    fun `buildOptions - 4 opsi, jawaban benar ada, tanpa duplikat`() {
        val random = Random(7)
        repeat(20) {
            val options = TajwidQuiz.buildOptions("Mad Thabi'i", random)
            assertEquals(4, options.size)
            assertEquals(4, options.toSet().size) // tanpa duplikat
            assertTrue(options.contains("Mad Thabi'i"))
        }
    }

    @Test
    fun `isCorrect membandingkan nama aturan sasaran`() {
        // اللَّهِ ditulis escape (shaddah sebelum fatha) agar urutan byte stabil.
        val q = TajwidQuiz.pickWord(listOf("\u0627\u0644\u0644\u0651\u064E\u0647\u0650"), Random(3))!!
        assertTrue(TajwidQuiz.isCorrect(q.targetRule.name, q))
        assertFalse(TajwidQuiz.isCorrect("Jawaban Salah", q))
    }

    @Test
    fun `opsi tidak memuat hukum lain yang juga benar pada kata itu`() {
        // إِنَّا (akhir ayat, tanpa kata berikut): ghunnah + mad thabi'i + mad aridh.
        val words = listOf("\u0625\u0646\u0651\u064E\u0627")
        val q = TajwidQuiz.pickWord(words, Random(0)) ?: throw AssertionError("harus ada soal")
        // Target = ghunnah (kategori pertama yang menarik); pengecoh TIDAK boleh
        // memuat mad thabi'i/aridh yang juga benar pada kata ini.
        assertEquals("Ghunnah (Mushaddad)", q.targetRule.name)
        assertFalse(q.options.contains("Mad Thabi'i"))
        assertFalse(q.options.contains("Mad Aridh Lis-Sukun"))
        assertTrue(q.options.contains("Ghunnah (Mushaddad)"))
        assertEquals(4, q.options.size)
    }

    @Test
    fun `soal konsisten untuk seed yang sama`() {
        val a = TajwidQuiz.pickWord(listOf("الرَّحْمَٰنِ", "الرَّحِيمِ"), Random(42))
        val b = TajwidQuiz.pickWord(listOf("الرَّحْمَٰنِ", "الرَّحِيمِ"), Random(42))
        assertNotNull(a)
        assertNotNull(b)
        assertEquals(a!!.word, b!!.word)
        assertEquals(a.targetRule.name, b.targetRule.name)
        assertEquals(a.options, b.options)
    }
}
