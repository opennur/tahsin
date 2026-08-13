package com.tahsin.app.util

import com.tahsin.app.data.vocab.VocabCard
import com.tahsin.app.data.vocab.VocabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes katalog badge & evaluator unlock (murni — tanpa Android). */
class AchievementsTest {

    private fun profile(
        xp: Int = 0,
        streak: Int = 0,
        ayahAttempts: Int = 0,
        bestAyahScore: Int = 0,
        wordsMastered: Int = 0,
        dreamBigBest: Int = 0,
        dreamBigRounds: Int = 0,
        lughohRounds: Int = 0,
        surahsCompleted: Int = 0,
    ) = PlayerProfile(
        xp = xp,
        streak = streak,
        level = Gamification.levelFor(xp),
        ayahAttempts = ayahAttempts,
        bestAyahScore = bestAyahScore,
        wordsMastered = wordsMastered,
        dreamBigBest = dreamBigBest,
        dreamBigRounds = dreamBigRounds,
        lughohRounds = lughohRounds,
        surahsCompleted = surahsCompleted,
    )

    // ---- profileOf ----

    @Test
    fun `profileOf - agregasi semua store`() {
        val reading = listOf(
            AyahStats(surahNumber = 1, ayahNumber = 1, attempts = 3, bestScore = 80),
            AyahStats(surahNumber = 1, ayahNumber = 2, attempts = 2, bestScore = 95),
        )
        val vocab = VocabState(
            cards = mapOf(
                "a" to VocabCard(key = "a", correctCount = 5),
                "b" to VocabCard(key = "b", correctCount = 0),
            ),
        )
        val p = Achievements.profileOf(
            readingStats = reading,
            vocab = vocab,
            dream = DreamBigStats(bestScore = 10, roundsPlayed = 12),
            lughoh = LughohStats(bestScore = 6, roundsPlayed = 4),
            gamification = GamificationStats(xp = 250, streak = 3),
            ayahCounts = mapOf(1 to 2),
        )
        assertEquals(5, p.ayahAttempts)
        assertEquals(95, p.bestAyahScore)
        assertEquals(1, p.wordsMastered)
        assertEquals(10, p.dreamBigBest)
        assertEquals(12, p.dreamBigRounds)
        assertEquals(4, p.lughohRounds)
        assertEquals(2, p.level)
        assertEquals(1, p.surahsCompleted) // surah 1: ayat 1 & 2 keduanya pernah dibaca
    }

    @Test
    fun `profileOf - surah belum tuntas bila ada ayat belum dibaca`() {
        val reading = listOf(
            AyahStats(surahNumber = 2, ayahNumber = 1, attempts = 1),
            AyahStats(surahNumber = 2, ayahNumber = 3, attempts = 1),
        )
        val p = Achievements.profileOf(
            readingStats = reading,
            vocab = VocabState(),
            dream = DreamBigStats(),
            lughoh = LughohStats(),
            gamification = GamificationStats(),
            ayahCounts = mapOf(2 to 4),
        )
        assertEquals(0, p.surahsCompleted) // ayat 2 & 4 belum pernah dibaca
    }

    // ---- newlyEarned ----

    @Test
    fun `newlyEarned - profil kosong - tidak ada badge`() {
        val earned = Achievements.newlyEarned(profile(), emptySet())
        assertTrue(earned.isEmpty())
    }

    @Test
    fun `newlyEarned - hanya badge yang kondisinya terpenuhi`() {
        val earned = Achievements.newlyEarned(
            profile(xp = 150, streak = 4, ayahAttempts = 12),
            emptySet(),
        )
        val keys = earned.map { it.key }
        assertTrue("first-step" in keys)
        assertTrue("streak-3" in keys)
        assertTrue("level-2" in keys)
        assertTrue("tahsin-10" in keys)
        assertFalse("streak-7" in keys)
        assertFalse("level-5" in keys)
        assertFalse("vocab-50" in keys)
    }

    @Test
    fun `newlyEarned - badge yang sudah diraih tidak diulang`() {
        val earned = Achievements.newlyEarned(profile(xp = 150), setOf("first-step", "level-2"))
        val keys = earned.map { it.key }
        assertFalse("first-step" in keys)
        assertFalse("level-2" in keys)
        assertTrue(keys.isEmpty()) // semua badge yang terpenuhi sudah diraih
    }

    @Test
    fun `badge - vocab-50 terpenuhi saat 50 kata dikuasai`() {
        val earned = Achievements.newlyEarned(profile(wordsMastered = 50), emptySet())
        assertTrue(earned.any { it.key == "vocab-50" })
    }

    @Test
    fun `badge - dream-perfect butuh skor ronde penuh`() {
        val p = profile(dreamBigBest = 9)
        assertFalse(Achievements.newlyEarned(p, emptySet()).any { it.key == "dream-perfect" })
        val full = profile(dreamBigBest = 10)
        assertTrue(Achievements.newlyEarned(full, emptySet()).any { it.key == "dream-perfect" })
    }

    @Test
    fun `badge - tahsin-perfect butuh skor ayat 90+`() {
        assertFalse(Achievements.newlyEarned(profile(bestAyahScore = 89), emptySet()).any { it.key == "tahsin-perfect" })
        assertTrue(Achievements.newlyEarned(profile(bestAyahScore = 90), emptySet()).any { it.key == "tahsin-perfect" })
    }

    @Test
    fun `badge - surah-complete butuh minimal satu surah tuntas`() {
        assertFalse(Achievements.newlyEarned(profile(surahsCompleted = 0), emptySet()).any { it.key == "surah-complete" })
        assertTrue(Achievements.newlyEarned(profile(surahsCompleted = 1), emptySet()).any { it.key == "surah-complete" })
    }

    @Test
    fun `byKey - key tak dikenal - null`() {
        assertEquals(null, Achievements.byKey("tidak-ada"))
        assertEquals("first-step", Achievements.byKey("first-step")?.key)
    }
}
