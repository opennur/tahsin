package com.tahsin.app.util

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
}
