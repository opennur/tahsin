package org.opennur.tahsin.stt

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
        // sim("ab","abcd") = 1 - 2/4 = 0.5 → di antara READING (0.35) dan MATCH (0.6).
        val result = TranscriptAligner.align("ab", listOf("abcd"))
        assertEquals(WordStatus.READING, result.single().status)
        assertEquals("ab", result.single().spokenWord)
    }

    @Test
    fun `transkrip kosong dan acuan kosong - hasil kosong`() {
        assertTrue(TranscriptAligner.align("", emptyList()).isEmpty())
    }

    @Test
    fun `kata yang cocok dengan kata acuan kedua - kata pertama SKIPPED (greedy)`() {
        // "b" hanya cocok dengan acuan kedua; greedy memakai b untuk acuan
        // pertama (MISMATCH) sehingga acuan kedua tak tercapai → SKIPPED.
        val result = TranscriptAligner.align("b", listOf("a", "b"))
        assertEquals(
            listOf(WordStatus.MISMATCH, WordStatus.SKIPPED),
            result.map { it.status },
        )
    }

    @Test
    fun `similarity - satu huruf vs tiga huruf = 0`() {
        assertEquals(0.0, TranscriptAligner.similarity("a", "xyz"), 0.0001)
    }

    @Test
    fun `similarity - huruf sama dengan urutan beda`() {
        assertEquals(1.0 - 2.0 / 3.0, TranscriptAligner.similarity("abc", "acb"), 0.0001)
    }

    @Test
    fun `align - kata ekstra di tengah membuat acuan berikutnya SKIPPED`() {
        // "a x b": x tidak cocok dengan b; greedy melompati acuan b (SKIPPED)
        // daripada mengonsumsi x — kata ekstra tidak menggeser yang sudah cocok.
        val result = TranscriptAligner.align("a x b", listOf("a", "b"))
        assertEquals(
            listOf(WordStatus.CORRECT, WordStatus.SKIPPED),
            result.map { it.status },
        )
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


    @Test
    fun `align - ucapan kosong - semua belum terbaca`() {
        val out = TranscriptAligner.align("   ", listOf("الرَّحْمَٰنِ", "الرَّحِيمِ"))
        assertEquals(2, out.size)
        assertTrue(out.all { it.status == org.opennur.tahsin.stt.WordStatus.NOT_REACHED })
    }

    @Test
    fun `align - acuan kosong - hasil kosong`() {
        assertTrue(TranscriptAligner.align("بسم الله", emptyList()).isEmpty())
    }


    @Test
    fun `align - kata acuan dilewati karena kata ucapan ekstra - SKIPPED`() {
        val out = TranscriptAligner.align("أ ب ج", listOf("أ", "ج"))
        assertEquals(2, out.size)
        assertEquals(org.opennur.tahsin.stt.WordStatus.CORRECT, out[0].status)
        assertEquals(org.opennur.tahsin.stt.WordStatus.SKIPPED, out[1].status)
    }

    @Test
    fun `levenshtein - edge string kosong dan identik`() {
        assertEquals(3, TranscriptAligner.levenshtein("", "abc"))
        assertEquals(2, TranscriptAligner.levenshtein("ab", ""))
        assertEquals(0, TranscriptAligner.levenshtein("", ""))
        assertEquals(0, TranscriptAligner.levenshtein("kata", "kata"))
        assertEquals(1, TranscriptAligner.levenshtein("kata", "kaza"))
    }
}
