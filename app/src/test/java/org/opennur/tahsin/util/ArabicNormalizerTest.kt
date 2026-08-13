package org.opennur.tahsin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes utilitas teks Arab: normalisasi + tokenisasi (dasar STT & tajwid). */
class ArabicNormalizerTest {

    @Test
    fun `stripMarks membuang harakat dan tanda mushaf`() {
        assertEquals("بسم", ArabicNormalizer.stripMarks("بِسْمِ"))
        assertEquals("الله", ArabicNormalizer.stripMarks("اللَّهِ"))
        assertEquals("الرحمن", ArabicNormalizer.stripMarks("الرَّحْمَٰنِ")) // alif khanjariah dibuang
    }

    @Test
    fun `normalize menyeragamkan hamza, ya, ta marbuta dan tatweel`() {
        assertEquals("ان", ArabicNormalizer.normalize("إِنَّ"))
        assertEquals("ان الحمد", ArabicNormalizer.normalize("إِنَّ الْحَمْدُ"))
        assertEquals("مريم", ArabicNormalizer.normalize("مَرْيَمَ"))
        assertEquals("صل", ArabicNormalizer.normalize("صَلّـ")) // tatweel dibuang
    }

    @Test
    fun `normalize mempertahankan konsonan yang tidak berubah`() {
        assertEquals("بسم الله الرحمن", ArabicNormalizer.normalize("بِسْمِ اللَّهِ الرَّحْمَٰنِ"))
    }

    @Test
    fun `isLetter hanya untuk huruf dasar Al-Quran`() {
        assertTrue(ArabicNormalizer.isLetter('ب'))
        assertTrue(ArabicNormalizer.isLetter('ء'))
        assertTrue(ArabicNormalizer.isLetter('إ'))
        assertFalse(ArabicNormalizer.isLetter('َ'))   // fatha
        assertFalse(ArabicNormalizer.isLetter(' '))
        assertFalse(ArabicNormalizer.isLetter('۝'))   // penanda akhir ayat (U+06DD)
    }

    @Test
    fun `splitWords membuang penanda waqaf dan token non-huruf`() {
        val words = ArabicNormalizer.splitWords("بِسْمِ \u06DD اللَّهِ الرَّحْمَٰنِ")
        assertEquals(listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَٰنِ"), words)
    }

    @Test
    fun `splitWords menangani spasi ganda dan kosong`() {
        assertEquals(emptyList<String>(), ArabicNormalizer.splitWords("   "))
        assertEquals(listOf("بسم"), ArabicNormalizer.splitWords("  بسم  "))
    }

    @Test
    fun `splitWords membuang token yang hanya tatweel`() {
        assertEquals(emptyList<String>(), ArabicNormalizer.splitWords("ـــ"))
        // Tatweel di dalam kata ikut dipertahankan (token utuh ber-tashkeel).
        assertEquals(listOf("صَلّـ"), ArabicNormalizer.splitWords("صَلّـ"))
    }

    @Test
    fun `normalize - string kosong tetap kosong`() {
        assertEquals("", ArabicNormalizer.normalize(""))
        assertEquals("", ArabicNormalizer.normalize("   "))
        assertEquals("", ArabicNormalizer.normalize("\u064B\u0651\u06D8")) // hanya tanda
    }

    @Test
    fun `normalize - hamza berkursi tidak dilipat (hanya pencarian yang melipat)`() {
        // ArabicNormalizer (dipakai STT) membuang hamza "ء" tapi MEMPERTAHANKAN
        // hamza berkursi ؤ/ئ — pelipatan itu khusus AyahSearch.searchNormalize.
        assertEquals("مؤمن", ArabicNormalizer.normalize("مُؤْمِنَ"))
    }

    @Test
    fun `normalize - semua bentuk hamza menjadi alif atau dibuang`() {
        assertEquals("ا", ArabicNormalizer.normalize("أ"))
        assertEquals("ا", ArabicNormalizer.normalize("إ"))
        assertEquals("ا", ArabicNormalizer.normalize("آ"))
        assertEquals("ا", ArabicNormalizer.normalize("\u0671")) // wasla ٱ
        assertEquals("", ArabicNormalizer.normalize("ء"))
    }

    @Test
    fun `stripMarks membuang tanda mushaf extended`() {
        // U+08D6 (ࣖ) termasuk rentang tanda mushaf extended yang ikut dibuang.
        assertEquals("بسم", ArabicNormalizer.stripMarks("بِسْمِ\u08D6"))
        assertEquals("قل", ArabicNormalizer.stripMarks("قُلْ\u06D8")) // tanda waqaf
    }

    @Test
    fun `isLetter - huruf dasar dan hamza benar`() {
        val letters = "ابتثجحخدذرزسشصضطظعغفقكلمنهويئةءأآإى"
        letters.forEach { assertTrue("harus huruf: $it", ArabicNormalizer.isLetter(it)) }
        val nonLetters = "َُِّْٰ \t\n\u06DD\u0671"
        nonLetters.forEach { assertFalse("bukan huruf: $it", ArabicNormalizer.isLetter(it)) }
    }
}
