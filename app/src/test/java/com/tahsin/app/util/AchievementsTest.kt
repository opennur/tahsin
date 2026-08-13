package com.tahsin.app.util

import com.tahsin.app.data.vocab.VocabCard
import com.tahsin.app.data.vocab.VocabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes badge progresif (metrik + tier tak terbatas) — murni, tanpa Android. */
class AchievementsTest {

    private fun profile(
        xp: Int = 0,
        streak: Int = 0,
        ayahAttempts: Int = 0,
        perfectAyahs: Int = 0,
        wordsMastered: Int = 0,
        dreamBigRounds: Int = 0,
        lughohRounds: Int = 0,
        surahsCompleted: Int = 0,
    ) = PlayerProfile(
        xp = xp,
        streak = streak,
        ayahAttempts = ayahAttempts,
        perfectAyahs = perfectAyahs,
        wordsMastered = wordsMastered,
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
        assertEquals(1, p.perfectAyahs) // hanya ayat 2 (95 ≥ 90)
        assertEquals(1, p.wordsMastered)
        assertEquals(12, p.dreamBigRounds)
        assertEquals(4, p.lughohRounds)
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
        assertEquals(0, p.surahsCompleted)
    }

    // ---- highestReachableTier (tak terbatas) ----

    @Test
    fun `highestReachableTier - xp mengikuti kurva level`() {
        val xpBadge = Achievements.byKey("xp")!!
        assertEquals(0, Achievements.highestReachableTier(xpBadge, 0))
        assertEquals(1, Achievements.highestReachableTier(xpBadge, 100))
        assertEquals(1, Achievements.highestReachableTier(xpBadge, 399))
        assertEquals(2, Achievements.highestReachableTier(xpBadge, 400))
        assertEquals(6, Achievements.highestReachableTier(xpBadge, 3600)) // 100·6²=3600 → tier 6
    }

    @Test
    fun `highestReachableTier - vocab 50 per tier, naik terus`() {
        val vocab = Achievements.byKey("vocab")!!
        assertEquals(0, Achievements.highestReachableTier(vocab, 49))
        assertEquals(1, Achievements.highestReachableTier(vocab, 50))
        assertEquals(2, Achievements.highestReachableTier(vocab, 100))
        assertEquals(5, Achievements.highestReachableTier(vocab, 260)) // 50·5=250 ≤ 260 < 300
    }

    // ---- newlyUnlocked ----

    @Test
    fun `newlyUnlocked - profil kosong - tidak ada`() {
        assertTrue(Achievements.newlyUnlocked(profile(), emptyMap()).isEmpty())
    }

    @Test
    fun `newlyUnlocked - buka beberapa badge sekaligus dengan tier masing-masing`() {
        val unlocked = Achievements.newlyUnlocked(
            profile(xp = 450, streak = 7, ayahAttempts = 25, perfectAyahs = 2, wordsMastered = 120),
            emptyMap(),
        )
        assertEquals(2, unlocked["xp"])        // 400 ≤ 450 < 900
        assertEquals(2, unlocked["streak"])    // 3·2=6 ≤ 7 < 9
        assertEquals(2, unlocked["tahsin"])    // 10·2=20 ≤ 25 < 30
        assertEquals(2, unlocked["tahsin-perfect"]) // tier = jumlah perfect (2)
        assertEquals(2, unlocked["vocab"])     // 50·2=100 ≤ 120 < 150
        assertFalse("dream" in unlocked)   // belum main → tidak ikut
    }

    @Test
    fun `newlyUnlocked - tier yang sudah diraih tidak diulang`() {
        val unlocked = Achievements.newlyUnlocked(
            profile(xp = 450),
            mapOf("xp" to 2, "streak" to 1),
        )
        assertFalse("xp" in unlocked) // sudah tier 2 = maksimal
        assertFalse("streak" in unlocked) // streak 0 < ambang tier 1
    }

    @Test
    fun `newlyUnlocked - naik multi-tier dalam satu evaluasi`() {
        // Dari tier 1 → langsung tier 5 (metrik sudah jauh melewati ambang).
        val unlocked = Achievements.newlyUnlocked(
            profile(wordsMastered = 260),
            mapOf("vocab" to 1),
        )
        assertEquals(5, unlocked["vocab"])
    }

    // ---- progressFor ----

    @Test
    fun `progressFor - tier saat ini, nilai, dan ambang berikutnya`() {
        val p = Achievements.progressFor(
            Achievements.byKey("vocab")!!,
            profile(wordsMastered = 120),
            earnedTier = 2,
        )
        assertEquals(2, p.currentTier)
        assertTrue(p.isUnlocked)
        assertEquals(120, p.metricValue)
        assertEquals(150, p.nextThreshold) // 50·3
        assertEquals(120f / 150f, p.fraction, 0.001f)
    }

    @Test
    fun `progressFor - terkunci menuju ambang pertama`() {
        val p = Achievements.progressFor(
            Achievements.byKey("vocab")!!,
            profile(wordsMastered = 25),
            earnedTier = 0,
        )
        assertFalse(p.isUnlocked)
        assertEquals(0, p.currentTier)
        assertEquals(50, p.nextThreshold)
    }

    // ---- katalog ----

    @Test
    fun `katalog - 8 badge progresif, byKey untuk yang tak dikenal = null`() {
        assertEquals(8, Achievements.ALL.size)
        assertTrue(Achievements.ALL.all { it.key.isNotBlank() && it.emoji.isNotBlank() })
        assertEquals(null, Achievements.byKey("tidak-ada"))
    }
}
