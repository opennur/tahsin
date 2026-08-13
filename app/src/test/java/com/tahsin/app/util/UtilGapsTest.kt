package com.tahsin.app.util

import com.tahsin.app.stt.AlignedWord
import com.tahsin.app.stt.WordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gap-coverage util murni: ReadingStats.merge, AyahSearch, AudioUrls. */
class UtilGapsTest {

    // ---- ReadingStats.merge ----

    private fun merge(
        existing: AyahStats?,
        aligned: List<AlignedWord>,
        referenceWords: List<String>,
        now: Long,
    ): AyahStats = ReadingStats.merge(
        surahNumber = 1, ayahNumber = 1, existing = existing,
        aligned = aligned, referenceWords = referenceWords, now = now,
    )

    @Test
    fun `merge - base kosong (existing null) - dibuat baru`() {
        val merged = merge(
            existing = null,
            aligned = listOf(AlignedWord(0, "الرَّحْمَٰنِ", WordStatus.CORRECT, "الرَّحْمَٰنِ")),
            referenceWords = listOf("الرَّحْمَٰنِ"),
            now = 100L,
        )
        assertEquals(1, merged.attempts)
        assertEquals(100, merged.lastScore)
        assertEquals(100, merged.bestScore)
        assertEquals(0, merged.wordErrors.size)
    }

    @Test
    fun `merge - kata salah tanpa kata rujukan - label elipsis`() {
        val merged = merge(
            existing = null,
            aligned = listOf(AlignedWord(0, "", WordStatus.MISMATCH, "salah")),
            referenceWords = listOf(""),
            now = 100L,
        )
        assertEquals(1, merged.wordErrors.size)
        assertFalse(merged.wordErrors[0].word.isBlank())
        assertEquals(1, merged.wordErrors[0].errorCount)
    }

    @Test
    fun `merge - kata salah dengan rujukan kosong memakai fallback index`() {
        val merged = merge(
            existing = null,
            aligned = listOf(AlignedWord(0, "", WordStatus.MISMATCH, "salah")),
            referenceWords = listOf("الرَّحْمَٰنِ"),
            now = 100L,
        )
        assertEquals("الرَّحْمَٰنِ", merged.wordErrors[0].word)
    }

    @Test
    fun `merge - akumulasi error lintas sesi`() {
        val first = merge(
            existing = null,
            aligned = listOf(AlignedWord(0, "كلمة", WordStatus.MISMATCH, "x")),
            referenceWords = listOf("كلمة"),
            now = 1L,
        )
        val second = merge(
            existing = first,
            aligned = listOf(
                AlignedWord(0, "كلمة", WordStatus.CORRECT, "كلمة"),
                AlignedWord(1, "خطأ", WordStatus.MISMATCH, "y"),
            ),
            referenceWords = listOf("كلمة", "خطأ"),
            now = 2L,
        )
        assertEquals(2, second.attempts)
        assertEquals(50, second.bestScore)
        assertEquals(2, second.wordErrors.size) // 1 dari sesi pertama + 1 baru
        assertEquals(1, second.wordErrors.first().errorCount) // keduanya 1x
        assertEquals(25, second.avgScore) // (0 + 50) / 2
    }

    // ---- AyahSearch.matches ----

    @Test
    fun `matches - query kosong atau hanya harakat - tidak cocok`() {
        assertFalse(AyahSearch.matches("أي نص", "teks", "text", ""))
        assertFalse(AyahSearch.matches("أي نص", "teks", "text", "   "))
        assertFalse(AyahSearch.matches("أي نص", "teks", "text", "ٌّ"))
    }

    @Test
    fun `matches - pencarian arab dan latin`() {
        assertTrue(AyahSearch.matches("الرَّحْمَٰنِ", "Yang Maha", "Most Gracious", "الرحمن"))
        assertTrue(AyahSearch.matches("الرَّحْمَٰنِ", "Al-Rahman", "Al-Rahman", "rahm"))
        assertFalse(AyahSearch.matches("الرَّحْمَٰنِ", "teks", "text", "xyz"))
    }

    // ---- AudioUrls ----

    @Test
    fun `isAyahAudioFileName - nama tak valid`() {
        assertFalse(AudioUrls.isAyahAudioFileName("001a.mp3")) // bukan digit
        assertFalse(AudioUrls.isAyahAudioFileName("001.mp4"))  // bukan .mp3
        assertFalse(AudioUrls.isAyahAudioFileName("001.mp3x"))
    }


    @Test
    fun `merge - index kata salah di luar daftar rujukan - aman`() {
        val merged = merge(
            existing = null,
            aligned = listOf(AlignedWord(5, "", WordStatus.MISMATCH, "x")),
            referenceWords = listOf("hanya-satu"),
            now = 1L,
        )
        assertEquals(1, merged.wordErrors.size)
        assertEquals(5, merged.wordErrors[0].wordIndex)
        assertEquals("…", merged.wordErrors[0].word)
    }

    @Test
    fun `merge - kata salah berulang dengan label lama kosong - pakai kata baru`() {
        // Data lama (JSON legacy) bisa berisi label kosong → diganti label baru.
        val legacy = AyahStats(
            surahNumber = 1, ayahNumber = 1, attempts = 1,
            wordErrors = listOf(WordError(0, "", 1)),
        )
        val second = merge(
            existing = legacy,
            aligned = listOf(AlignedWord(0, "kata-baru", WordStatus.MISMATCH, "y")),
            referenceWords = listOf(""),
            now = 2L,
        )
        assertEquals(2, second.wordErrors[0].errorCount)
        assertEquals("kata-baru", second.wordErrors[0].word)
    }


    @Test
    fun `isAyahAudioFileName - valid dan panjang tidak enam`() {
        assertTrue(AudioUrls.isAyahAudioFileName("001234.mp3"))
        assertFalse(AudioUrls.isAyahAudioFileName("001.mp3"))      // panjang 3
        assertFalse(AudioUrls.isAyahAudioFileName("1234567.mp3"))  // panjang 7
    }


    @Test
    fun `isAyahAudioFileName - base enam dengan huruf di tengah`() {
        assertFalse(AudioUrls.isAyahAudioFileName("00123a.mp3")) // panjang 6 tapi bukan digit semua
    }
}
