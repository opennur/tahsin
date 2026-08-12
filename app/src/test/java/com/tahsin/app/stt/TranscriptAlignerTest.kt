package com.tahsin.app.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes penyelarasan transkrip STT dengan kata acuan (level kata, Levenshtein).
 *
 * String ASCII dipakai untuk menguji algoritma (bukan teks Arab); beberapa
 * kasus Arab menegaskan normalisasi harakat sebelum pencocokan.
 */
class TranscriptAlignerTest {

    @Test
    fun `transkrip kosong - semua kata NOT_REACHED`() {
        val result = TranscriptAligner.align("", listOf("a", "b", "c"))
        assertEquals(
            listOf(WordStatus.NOT_REACHED, WordStatus.NOT_REACHED, WordStatus.NOT_REACHED),
            result.map { it.status },
        )
    }

    @Test
    fun `bacaan lengkap benar - semua CORRECT dengan kata ucapan`() {
        val result = TranscriptAligner.align("a b c", listOf("a", "b", "c"))
        assertTrue(result.all { it.status == WordStatus.CORRECT })
        assertEquals(listOf("a", "b", "c"), result.map { it.spokenWord })
    }

    @Test
    fun `kata tengah terlewat - kata sebelumnya MISMATCH, sisanya SKIPPED`() {
        // "a c": b tidak cocok dengan c (MISMATCH), c tidak pernah tercapai (SKIPPED).
        val result = TranscriptAligner.align("a c", listOf("a", "b", "c"))
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.MISMATCH, WordStatus.SKIPPED),
            result.map { it.status },
        )
    }

    @Test
    fun `kata terakhir belum terbaca - SKIPPED`() {
        val result = TranscriptAligner.align("a b", listOf("a", "b", "c"))
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.CORRECT, WordStatus.SKIPPED),
            result.map { it.status },
        )
    }

    @Test
    fun `bacaan salah semua - MISMATCH semua`() {
        val result = TranscriptAligner.align("x y z", listOf("a", "b", "c"))
        assertTrue(result.all { it.status == WordStatus.MISMATCH })
    }

    @Test
    fun `kata ekstra diabaikan`() {
        val result = TranscriptAligner.align("a b c d", listOf("a", "b"))
        assertTrue(result.all { it.status == WordStatus.CORRECT })
    }

    @Test
    fun `kemiripan parsial - READING`() {
        val result = TranscriptAligner.align("ab", listOf("abcd"))
        assertEquals(WordStatus.READING, result.single().status)
        assertEquals("ab", result.single().spokenWord)
    }

    @Test
    fun `kata awal ekstra membuat kata acuan SKIPPED (greedy)`() {
        val result = TranscriptAligner.align("x a", listOf("a", "b"))
        assertEquals(
            listOf(WordStatus.SKIPPED, WordStatus.MISMATCH),
            result.map { it.status },
        )
    }

    @Test
    fun `harakat dibuang sebelum pencocokan - Arab dibaca benar`() {
        val result = TranscriptAligner.align("بِسْمِ اللَّهِ", listOf("بسم", "الله"))
        assertTrue(result.all { it.status == WordStatus.CORRECT })
    }

    @Test
    fun `hamza dan varian huruf dinormalisasi`() {
        // "إِنَّ" → normalisasi "ان", "مَرْيَمَ" → "مريم" (ى→ي juga ikut).
        val result = TranscriptAligner.align("ان مريم", listOf("إِنَّ", "مَرْيَمَ"))
        assertTrue(result.all { it.status == WordStatus.CORRECT })
    }

    // ---- similarity (Levenshtein) ----

    @Test
    fun `similarity - string identik = 1`() {
        assertEquals(1.0, TranscriptAligner.similarity("abc", "abc"), 0.0001)
    }

    @Test
    fun `similarity - satu huruf beda`() {
        assertEquals(1.0 - 1.0 / 3.0, TranscriptAligner.similarity("abc", "abd"), 0.0001)
    }

    @Test
    fun `similarity - string kosong = 0`() {
        assertEquals(0.0, TranscriptAligner.similarity("", "abc"), 0.0001)
        assertEquals(0.0, TranscriptAligner.similarity("abc", ""), 0.0001)
    }

    @Test
    fun `similarity - sangat berbeda mendekati 0`() {
        assertEquals(0.0, TranscriptAligner.similarity("abc", "xyz"), 0.0001)
    }
}
