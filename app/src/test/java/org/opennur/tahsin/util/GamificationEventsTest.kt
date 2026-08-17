package org.opennur.tahsin.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tes event bus perayaan gamification (post/consume, slot tunggal). */
class GamificationEventsTest {

    @After
    fun tearDown() {
        GamificationEvents.endSuppression()
        GamificationEvents.consume()
    }

    @Test
    fun `event - awal kosong`() {
        assertNull(GamificationEvents.event.value)
    }

    @Test
    fun `post - event tersimpan`() {
        val e = CelebrationEvent(CelebrationType.LEVEL_UP, level = 3)
        GamificationEvents.post(e)
        assertEquals(e, GamificationEvents.event.value)
    }

    @Test
    fun `post - event terbaru menimpa yang lama (slot tunggal)`() {
        GamificationEvents.post(CelebrationEvent(CelebrationType.LEVEL_UP, level = 2))
        val latest = CelebrationEvent(CelebrationType.BADGE_EARNED, badgeKey = "vocab", tier = 3)
        GamificationEvents.post(latest)
        assertEquals(latest, GamificationEvents.event.value)
    }

    @Test
    fun `consume - mengosongkan event`() {
        GamificationEvents.post(CelebrationEvent(CelebrationType.STREAK_MILESTONE, streak = 7))
        GamificationEvents.consume()
        assertNull(GamificationEvents.event.value)
    }

    @Test
    fun `suppress - event ditahan lalu muncul saat sesi selesai`() {
        val e = CelebrationEvent(CelebrationType.BADGE_EARNED, badgeKey = "tahsin-perfect", tier = 1)
        GamificationEvents.beginSuppression()
        GamificationEvents.post(e)

        assertNull(GamificationEvents.event.value)
        GamificationEvents.endSuppression()
        assertEquals(e, GamificationEvents.event.value)
    }

    @Test
    fun `suppress - hanya event terakhir yang ditampilkan`() {
        val first = CelebrationEvent(CelebrationType.LEVEL_UP, level = 2)
        val latest = CelebrationEvent(CelebrationType.STREAK_MILESTONE, streak = 7)
        GamificationEvents.beginSuppression()
        GamificationEvents.post(first)
        GamificationEvents.post(latest)
        GamificationEvents.endSuppression()

        assertEquals(latest, GamificationEvents.event.value)
    }

    @Test
    fun `suppress - consume membuang event tertunda`() {
        GamificationEvents.beginSuppression()
        GamificationEvents.post(CelebrationEvent(CelebrationType.LEVEL_UP, level = 4))
        GamificationEvents.consume()
        GamificationEvents.endSuppression()

        assertNull(GamificationEvents.event.value)
    }
}
