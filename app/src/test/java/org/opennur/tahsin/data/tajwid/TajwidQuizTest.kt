package org.opennur.tahsin.data.tajwid

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

    @Test
    fun `kata dengan hanya hukum membosankan tetap menghasilkan soal`() {
        // بَلَّدَ = ب(0) َ(1) ل(2) ّ(3) َ(4) د(5) َ(6) — hanya tasydid (kategori
        // boring; tanpa huruf isti'la/qalqalah/mad). Ditulis dengan \u escapes
        // agar urutan tanda tidak diubah urutan kanonik NFC.
        val q = TajwidQuiz.pickWord(listOf("\u0628\u064E\u0644\u0651\u064E\u062F\u064E"), Random(0))
        assertNotNull(q)
        assertEquals("Tasydid", q!!.targetRule.name)
        assertEquals(4, q.options.size)
        assertTrue(q.options.contains("Tasydid"))
    }

    @Test
    fun `pengecoh tidak memuat aturan yang dikecualikan`() {
        val exclude = listOf("Mad Thabi'i", "Iqlab", "Qalqalah")
        val random = Random(5)
        repeat(10) {
            val options = TajwidQuiz.buildOptions("Mad Wajib Muttasil", random, exclude)
            assertEquals(4, options.size)
            assertEquals(4, options.toSet().size)
            assertTrue(options.contains("Mad Wajib Muttasil"))
            assertFalse(options.any { it in exclude })
        }
    }

    @Test
    fun `soal selalu memakai kata dari daftar yang diberikan`() {
        val words = listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَٰنِ")
        repeat(10) {
            val q = TajwidQuiz.pickWord(words, Random(it)) ?: return@repeat
            assertTrue("kata di luar daftar: ${q.word}", q.word in words)
        }
    }

    @Test
    fun `opsi soal tidak pernah berisi jawaban ganda yang bisa dibela`() {
        // Untuk setiap kata ber-hukum: opsi ≠ nama aturan lain yang benar pada kata itu.
        val words = listOf("\u0625\u0646\u0651\u064E\u0627", "مِنْ", "قَالَ", "\u0627\u0644\u0644\u0651\u064E\u0647\u064F")
        repeat(20) {
            val q = TajwidQuiz.pickWord(words, Random(it)) ?: return@repeat
            val allRuleNames = TajwidEngine.analyzeWord(q.word, q.prevWord, q.nextWord)
                .map { it.name }
                .toSet()
            q.options.forEach { opt ->
                if (opt != q.targetRule.name) {
                    assertFalse("pengecoh $opt juga benar pada kata ini", opt in allRuleNames)
                }
            }
        }
    }


    // ---- gap coverage: default argumen (random/panjang) ----

    @Test
    fun `pickWord - tanpa random eksplisit - jalan`() {
        // Kata harus punya hukum tajwid agar jadi kandidat.
        val words = listOf("قَالَ", "رَبُّكُمُ", "وَمَا")
        val picked = TajwidQuiz.pickWord(words)
        assertNotNull(picked)
    }

    @Test
    fun `buildOptions - tanpa random eksplisit - jalan`() {
        val options = TajwidQuiz.buildOptions("مرحبا")
        assertNotNull(options)
    }

    @Test
    fun `pickWordAt mendukung target eksplisit dan indeks invalid`() {
        val words = listOf("\u0625\u0646\u0651\u064E\u0627")
        val explicit = TajwidQuiz.pickWordAt(
            words = words,
            index = 0,
            targetRuleName = "Ghunnah (Mushaddad)",
        )
        assertNotNull(explicit)
        assertEquals("Ghunnah (Mushaddad)", explicit!!.targetRule.name)

        val fallback = TajwidQuiz.pickWordAt(words, 0, targetRuleName = "Tidak ada")
        assertNotNull(fallback)
        assertTrue(TajwidQuiz.pickWordAt(words, 99) == null)
        assertTrue(TajwidQuiz.pickWordAt(listOf("مِنْ"), 0) == null)
    }
}
