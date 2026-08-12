package com.tahsin.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tes pemilihan "Ayah of the Day" — logika MURNI (tanpa Android). */
class AyahOfTheDayManagerTest {

    // 3 surah fiktif: 7, 286, 200 ayat → kumulatif [7, 293, 493]
    private val counts = listOf(7, 286, 200)
    private val cumulative = AyahOfTheDayManager.cumulativeCounts(counts)

    @Test
    fun `cumulativeCounts - prefix sum`() {
        assertEquals(listOf(7, 293, 493), cumulative)
    }

    @Test
    fun `refForIndex - ayat pertama mushaf`() {
        assertEquals(1 to 1, AyahOfTheDayManager.refForIndex(0, cumulative))
    }

    @Test
    fun `refForIndex - ayat terakhir surah pertama`() {
        assertEquals(1 to 7, AyahOfTheDayManager.refForIndex(6, cumulative))
    }

    @Test
    fun `refForIndex - melintasi batas surah`() {
        assertEquals(2 to 1, AyahOfTheDayManager.refForIndex(7, cumulative))
        assertEquals(2 to 286, AyahOfTheDayManager.refForIndex(292, cumulative))
        assertEquals(3 to 1, AyahOfTheDayManager.refForIndex(293, cumulative))
    }

    @Test
    fun `refForIndex - ayat terakhir mushaf`() {
        assertEquals(3 to 200, AyahOfTheDayManager.refForIndex(492, cumulative))
    }

    @Test
    fun `refForIndex - index di luar jangkauan ditolak`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayManager.refForIndex(-1, cumulative)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayManager.refForIndex(493, cumulative)
        }
    }

    @Test
    fun `refForIndex - daftar kumulatif kosong ditolak`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayManager.refForIndex(0, emptyList())
        }
    }

    @Test
    fun `ayahRefForDate - deterministik untuk tanggal yang sama`() {
        val day = LocalDate.of(2026, 8, 12).toEpochDay()
        assertEquals(
            AyahOfTheDayManager.ayahRefForDate(day, cumulative),
            AyahOfTheDayManager.ayahRefForDate(day, cumulative),
        )
    }

    @Test
    fun `ayahRefForDate - selalu dalam jangkauan untuk rentang tanggal`() {
        for (epochDay in 19_000L..19_200L) {
            val (surah, ayah) = AyahOfTheDayManager.ayahRefForDate(epochDay, cumulative)
            assertTrue("surah=$surah", surah in 1..counts.size)
            assertTrue("ayah=$ayah", ayah in 1..counts[surah - 1])
        }
    }

    @Test
    fun `ayahRefForDate - tanggal berbeda umumnya memberi ayat berbeda`() {
        val refs = (19_000L..19_060L).map { AyahOfTheDayManager.ayahRefForDate(it, cumulative) }.toSet()
        assertTrue("Hanya ${refs.size} ref unik dari 61 hari", refs.size > 1)
    }

    @Test
    fun `dateKey - format yyyy-MM-dd`() {
        assertEquals("2026-08-12", AyahOfTheDayManager.dateKey(LocalDate.of(2026, 8, 12)))
    }
}
