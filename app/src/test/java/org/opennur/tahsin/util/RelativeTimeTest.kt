package org.opennur.tahsin.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Tes RelativeTime — ditulis DULU (TDD red): perilaku label waktu relatif
 * untuk riwayat baca. `now` tetap (2026-08-14 10:00 zona lokal) supaya
 * deterministik.
 */
class RelativeTimeTest {

    private val now: Long = ZonedDateTime.of(2026, 8, 14, 10, 0, 0, 0, ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    @Test
    fun `kurang dari 1 menit - baru saja`() {
        assertEquals("baru saja", RelativeTime.format(now - 30_000L, now, AppLanguage.ID))
        assertEquals("just now", RelativeTime.format(now - 30_000L, now, AppLanguage.EN))
    }

    @Test
    fun `timestamp di masa depan dianggap baru saja`() {
        assertEquals("baru saja", RelativeTime.format(now + 5_000L, now, AppLanguage.ID))
    }

    @Test
    fun `menit - N mnt lalu`() {
        assertEquals("5 mnt lalu", RelativeTime.format(now - 5 * 60_000L, now, AppLanguage.ID))
        assertEquals("5 min ago", RelativeTime.format(now - 5 * 60_000L, now, AppLanguage.EN))
    }

    @Test
    fun `jam - N jam lalu`() {
        assertEquals("3 jam lalu", RelativeTime.format(now - 3 * 3_600_000L, now, AppLanguage.ID))
        assertEquals("3 h ago", RelativeTime.format(now - 3 * 3_600_000L, now, AppLanguage.EN))
    }

    @Test
    fun `kemarin (masuk hari sebelumnya)`() {
        assertEquals("kemarin", RelativeTime.format(now - 25 * 3_600_000L, now, AppLanguage.ID))
        assertEquals("yesterday", RelativeTime.format(now - 25 * 3_600_000L, now, AppLanguage.EN))
    }

    @Test
    fun `lebih tua - tanggal dengan bulan`() {
        // now - 3 hari = 11 Agu 2026.
        assertEquals("11 Agu", RelativeTime.format(now - 3 * 86_400_000L, now, AppLanguage.ID))
        assertEquals("Aug 11", RelativeTime.format(now - 3 * 86_400_000L, now, AppLanguage.EN))
    }
}
