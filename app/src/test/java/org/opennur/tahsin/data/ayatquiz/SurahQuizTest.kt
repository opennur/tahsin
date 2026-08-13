package org.opennur.tahsin.data.ayatquiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Tes engine "Tebak Surah" (murni). */
class SurahQuizTest {

    private val names = listOf(
        1 to "Al-Fatihah",
        2 to "Al-Baqarah",
        3 to "Ali 'Imran",
        4 to "An-Nisa'",
        5 to "Al-Ma'idah",
    )

    @Test
    fun `makeQuestion - potongan ayat & 4 opsi nama surah`() {
        val arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
        val q = SurahQuiz.makeQuestion(
            surahNumber = 1,
            ayahNumber = 1,
            arabic = arabic,
            surahNames = names,
            random = Random(5),
        )
        assertNotNull(q)
        q!!
        assertEquals(1, q.surahNumber)
        assertEquals("Al-Fatihah", q.correctName)
        assertEquals(4, q.options.size)
        assertTrue(q.correctName in q.options)
        // Fragment = 5 kata pertama.
        assertEquals("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ الْحَمْدُ", q.fragment)
        // Semua opsi selain benar adalah nama surah lain.
        assertTrue(q.options.filter { it != q.correctName }.all { it in names.map { n -> n.second }.filter { n -> n != "Al-Fatihah" } })
    }

    @Test
    fun `makeQuestion - surah tak dikenal - null`() {
        assertNull(
            SurahQuiz.makeQuestion(99, 1, "أ", names, Random(1)),
        )
    }

    @Test
    fun `makeQuestion - kolam nama kurang - null`() {
        assertNull(
            SurahQuiz.makeQuestion(1, 1, "أ", listOf(1 to "Al-Fatihah"), Random(1)),
        )
    }

    @Test
    fun `isCorrect - cocok dengan nama benar`() {
        val arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val q = SurahQuiz.makeQuestion(3, 1, arabic, names, Random(2))!!
        assertTrue(SurahQuiz.isCorrect("Ali 'Imran", q))
        assertFalse(SurahQuiz.isCorrect("Al-Baqarah", q))
    }


    @Test
    fun `model - getter ayahNumber konsisten`() {
        val q = SurahQuiz.makeQuestion(2, 7, "بِسْمِ اللَّهِ", names, Random(1))!!
        assertEquals(2, q.surahNumber)
        assertEquals(7, q.ayahNumber)
    }

    @Test
    fun `makeQuestion - tanpa random eksplisit - jalan`() {
        val q = SurahQuiz.makeQuestion(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", names)
        assertNotNull(q)
        assertEquals(4, q!!.options.size)
    }


    @Test
    fun `makeQuestion - nama surah kosong dibuang dari opsi`() {
        val withBlank = names + (99 to "   ")
        val q = SurahQuiz.makeQuestion(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", withBlank, Random(1))
        assertNotNull(q)
        assertTrue(q!!.options.none { it.isBlank() })
    }


    @Test
    fun `makeQuestion - surah yang dicari bukan elemen pertama`() {
        val q = SurahQuiz.makeQuestion(2, 7, "بِسْمِ اللَّهِ", names, Random(1))
        assertNotNull(q)
        assertEquals(names.first { it.first == 2 }.second, q!!.correctName)
    }

    @Test
    fun `makeQuestion - spasi ganda dalam ayat - fragment bersih`() {
        val q = SurahQuiz.makeQuestion(1, 1, "بِسْمِ  اللَّهِ  الرَّحْمَٰنِ", names, Random(1))
        assertNotNull(q)
        assertTrue(q!!.fragment.split(" ").none { it.isBlank() })
    }


    @Test
    fun `makeQuestion - arabic hanya spasi - fragment kosong aman`() {
        val q = SurahQuiz.makeQuestion(1, 1, "     ", names, Random(1))
        assertNotNull(q)
        assertTrue(q!!.fragment.isEmpty())
    }
}
