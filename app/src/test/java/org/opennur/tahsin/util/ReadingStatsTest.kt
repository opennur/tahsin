package org.opennur.tahsin.util

import org.opennur.tahsin.stt.AlignedWord
import org.opennur.tahsin.stt.WordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes logika statistik & riwayat bacaan (murni, tanpa Android):
 * skor, penggabungan per-percobaan, dan akumulasi kata yang sering salah.
 */
class ReadingStatsTest {

    private fun word(
        index: Int,
        ref: String,
        status: WordStatus,
        spoken: String? = null,
    ) = AlignedWord(index, ref, status, spoken)

    // ---- skor ----

    @Test
    fun `skor penuh saat semua kata benar`() {
        val aligned = listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.CORRECT, "b"),
            word(2, "c", WordStatus.CORRECT, "c"),
        )
        assertEquals(100, ReadingStats.scoreOf(aligned))
    }

    @Test
    fun `skor setengah saat separuh benar`() {
        val aligned = listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.MISMATCH, "x"),
        )
        assertEquals(50, ReadingStats.scoreOf(aligned))
    }

    @Test
    fun `skor nol untuk daftar kosong`() {
        assertEquals(0, ReadingStats.scoreOf(emptyList()))
    }

    @Test
    fun `kata terlewat dan belum terbaca tidak dihitung benar`() {
        val aligned = listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.SKIPPED),
            word(2, "c", WordStatus.NOT_REACHED),
        )
        assertEquals(33, ReadingStats.scoreOf(aligned))
    }

    // ---- merge ----

    @Test
    fun `percobaan pertama membuat statistik baru`() {
        val aligned = listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.MISMATCH, "x"),
        )
        val merged = ReadingStats.merge(1, 2, null, aligned, listOf("a", "b"), now = 1000L)

        assertEquals(1, merged.surahNumber)
        assertEquals(2, merged.ayahNumber)
        assertEquals(1, merged.attempts)
        assertEquals(50, merged.lastScore)
        assertEquals(50, merged.bestScore)
        assertEquals(50, merged.scoreSum)
        assertEquals(1000L, merged.lastPracticedAt)
        assertEquals(listOf(WordError(1, "b", 1)), merged.wordErrors)
    }

    @Test
    fun `percobaan kedua menggabung skor dan menaikkan hitung kesalahan`() {
        val first = ReadingStats.merge(1, 2, null, listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
            word(1, "b", WordStatus.CORRECT, "b"),
        ), listOf("a", "b"), now = 1L)

        val second = ReadingStats.merge(1, 2, first, listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.MISMATCH, "y"),
        ), listOf("a", "b"), now = 2L)

        assertEquals(2, second.attempts)
        assertEquals(100, second.scoreSum)         // 50 + 50
        assertEquals(50, second.avgScore)
        assertEquals(50, second.bestScore)
        assertEquals(50, second.lastScore)
        assertEquals(2L, second.lastPracticedAt)
        // "a" salah 1×, "b" salah 1× — terurut menurun (sama → urut index).
        assertEquals(listOf(WordError(0, "a", 1), WordError(1, "b", 1)), second.wordErrors)
    }

    @Test
    fun `skor terbaik dipertahankan saat percobaan lebih buruk`() {
        var stats = ReadingStats.merge(1, 1, null, listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.CORRECT, "b"),
        ), listOf("a", "b"), now = 1L)
        assertEquals(100, stats.bestScore)

        stats = ReadingStats.merge(1, 1, stats, listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
            word(1, "b", WordStatus.MISMATCH, "y"),
        ), listOf("a", "b"), now = 2L)

        assertEquals(100, stats.bestScore)
        assertEquals(0, stats.lastScore)
        assertEquals(50, stats.avgScore) // (100 + 0) / 2
        assertEquals(2, stats.attempts)
    }

    @Test
    fun `kata yang sama salah berulang - hitungnya bertambah`() {
        var stats: AyahStats? = null
        repeat(3) {
            stats = ReadingStats.merge(1, 7, stats, listOf(
                word(0, "allah", WordStatus.MISMATCH, "allahh"),
                word(1, "rahman", WordStatus.CORRECT, "rahman"),
            ), listOf("allah", "rahman"), now = it.toLong())
        }

        val result = stats!!
        assertEquals(3, result.attempts)
        assertEquals(50, result.avgScore)
        assertEquals(WordError(0, "allah", 3), result.wordErrors.single())
        // Hanya kata yang pernah salah yang tercatat (rahman tidak muncul).
    }

    @Test
    fun `kata benar kemudian salah - tetap tercatat setelah salah`() {
        var stats = ReadingStats.merge(2, 5, null, listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.CORRECT, "b"),
        ), listOf("a", "b"), now = 1L)
        assertEquals(0, stats.wordErrors.size)

        stats = ReadingStats.merge(2, 5, stats, listOf(
            word(0, "a", WordStatus.SKIPPED),
            word(1, "b", WordStatus.CORRECT, "b"),
        ), listOf("a", "b"), now = 2L)

        assertEquals(WordError(0, "a", 1), stats.wordErrors.single())
    }

    @Test
    fun `kesalahan dan terlewat dihitung sama sebagai satu kesalahan per kata`() {
        // Kata yang sama tidak boleh dihitung dua kali dalam satu percobaan
        // (groupBy index → satu entry per kata).
        val aligned = listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
            word(1, "b", WordStatus.SKIPPED),
            word(2, "c", WordStatus.CORRECT, "c"),
        )
        val stats = ReadingStats.merge(1, 1, null, aligned, listOf("a", "b", "c"), now = 1L)

        assertEquals(
            listOf(WordError(0, "a", 1), WordError(1, "b", 1)),
            stats.wordErrors,
        )
        assertEquals(33, stats.lastScore)
    }

    @Test
    fun `urutan kata salah - paling sering di depan`() {
        var stats: AyahStats? = null
        // Percobaan 1: kata 0 salah. Percobaan 2 & 3: kata 0 & 1 salah.
        stats = ReadingStats.merge(1, 1, stats, listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
            word(1, "b", WordStatus.CORRECT, "b"),
        ), listOf("a", "b"), now = 1L)
        stats = ReadingStats.merge(1, 1, stats, listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
            word(1, "b", WordStatus.MISMATCH, "y"),
        ), listOf("a", "b"), now = 2L)
        stats = ReadingStats.merge(1, 1, stats, listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
            word(1, "b", WordStatus.MISMATCH, "y"),
        ), listOf("a", "b"), now = 3L)

        val result = stats!!
        assertEquals(listOf(WordError(0, "a", 2), WordError(1, "b", 2)), result.wordErrors)
    }

    @Test
    fun `kata acuan kosong - fallback ke daftar kata acuan`() {
        val aligned = listOf(AlignedWord(0, "", WordStatus.SKIPPED))
        val stats = ReadingStats.merge(1, 1, null, aligned, listOf("rahman"), now = 1L)
        assertEquals(WordError(0, "rahman", 1), stats.wordErrors.single())
    }

    @Test
    fun `kata acuan dan daftar kata kosong - label fallback titik-tiga`() {
        val aligned = listOf(AlignedWord(0, "", WordStatus.SKIPPED))
        val stats = ReadingStats.merge(1, 1, null, aligned, emptyList(), now = 1L)
        assertEquals(WordError(0, "…", 1), stats.wordErrors.single())
    }

    @Test
    fun `kata yang belum terbaca (NOT_REACHED) tidak dihitung sebagai kesalahan`() {
        val aligned = listOf(
            word(0, "a", WordStatus.NOT_REACHED),
            word(1, "b", WordStatus.CORRECT, "b"),
            word(2, "c", WordStatus.READING, "c"),
        )
        val stats = ReadingStats.merge(1, 1, null, aligned, listOf("a", "b", "c"), now = 1L)
        assertTrue(stats.wordErrors.isEmpty())
        assertEquals(33, stats.lastScore) // hanya b yang benar
    }

    @Test
    fun `avgScore nol sebelum ada percobaan`() {
        assertEquals(0, AyahStats(1, 1).avgScore)
        assertEquals(0, AyahStats(1, 1, attempts = 0, scoreSum = 0).avgScore)
    }

    @Test
    fun `label kata dipertahankan walau percobaan berikutnya kosong`() {
        var stats = ReadingStats.merge(1, 1, null, listOf(
            word(0, "rahman", WordStatus.MISMATCH, "rahmaan"),
        ), listOf("rahman"), now = 1L)

        // Percobaan kedua: kata yang sama salah tapi label acuan kosong.
        stats = ReadingStats.merge(1, 1, stats, listOf(
            AlignedWord(0, "", WordStatus.SKIPPED),
        ), listOf("rahman"), now = 2L)

        assertEquals(WordError(0, "rahman", 2), stats.wordErrors.single())
    }

    @Test
    fun `merge tidak mengubah data lama (immutable)`() {
        val first = ReadingStats.merge(1, 1, null, listOf(
            word(0, "a", WordStatus.MISMATCH, "x"),
        ), listOf("a"), now = 1L)
        val second = ReadingStats.merge(1, 1, first, listOf(
            word(0, "a", WordStatus.CORRECT, "a"),
        ), listOf("a"), now = 2L)

        assertEquals(1, first.attempts)
        assertEquals(2, second.attempts)
        assertEquals(1, first.wordErrors.single().errorCount)
    }
}
