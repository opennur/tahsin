package com.tahsin.app.util

import android.content.Context
import com.tahsin.app.data.quran.QuranRepository

/**
 * Hub ekonomi game: satu titik agregasi "catat XP + cek badge".
 *
 * Dipanggil tiap event XP dari semua fitur ([award]); membaca store lain
 * hanya saat badge dievaluasi, jadi ViewModel tidak perlu tahu store lain.
 * Pola Context-based (bukan DI manual) — konsisten dengan gaya arsitektur
 * proyek (store Gson + filesDir).
 */
object GamificationHub {

    /**
     * Catat XP hari ini lalu cek badge baru (baca semua store; aman
     * dipanggil sering). Mengembalikan [ActivityResult] untuk deteksi
     * level-up di UI. Perayaan (level-up / streak / badge) diposting ke
     * [GamificationEvents] — dipanggil dari thread IO mana pun.
     */
    fun award(context: Context, xp: Int): ActivityResult {
        val app = context.applicationContext
        val g = GamificationStore.fromContext(app)
        val result = g.recordActivity(xp)
        val newTiers = checkAndUnlock(app, g)
        celebrate(result, newTiers)
        return result
    }

    /** Tingkat streak yang dirayakan (semakin tinggi semakin langka). */
    private val STREAK_MILESTONES = setOf(3, 7, 14, 30)

    /**
     * Post satu perayaan paling penting: level-up > streak milestone > badge.
     * Satu event per pemanggilan agar dialog tidak menumpuk.
     */
    private fun celebrate(result: ActivityResult, newTiers: Map<String, Int>) {
        if (result.leveledUp) {
            GamificationEvents.post(
                CelebrationEvent(
                    type = CelebrationType.LEVEL_UP,
                    level = Gamification.levelFor(result.after.xp),
                ),
            )
            return
        }
        if (result.after.streak in STREAK_MILESTONES && result.after.streak > result.before.streak) {
            GamificationEvents.post(
                CelebrationEvent(
                    type = CelebrationType.STREAK_MILESTONE,
                    streak = result.after.streak,
                ),
            )
            return
        }
        if (newTiers.isNotEmpty()) {
            val (key, tier) = newTiers.entries.first()
            GamificationEvents.post(
                CelebrationEvent(
                    type = CelebrationType.BADGE_EARNED,
                    badgeKey = key,
                    tier = tier,
                ),
            )
        }
    }

    /**
     * Evaluasi semua badge terhadap progres saat ini; badge yang baru
     * terpenuhi digabung ke [GamificationStats.badgeTiers] dan di-persist.
     * Mengembalikan daftar badge yang baru diraih (untuk notifikasi UI).
     */
    /**
     * Evaluasi semua badge terhadap progres saat ini; tier baru yang layak
     * dibuka digabung (keep max) ke [GamificationStats.badgeTiers] lalu
     * di-persist. Mengembalikan key → tier yang BARU dibuka (untuk perayaan).
     */
    fun checkAndUnlock(app: Context, g: GamificationStore = GamificationStore.fromContext(app)): Map<String, Int> =
        g.withWriteLock {
            val stats = g.read()
            val profile = loadProfile(app, stats)
            val unlocked = Achievements.newlyUnlocked(profile, stats.badgeTiers)
            if (unlocked.isNotEmpty()) {
                // Map + Map: sisi kanan menang (tier baru selalu lebih tinggi).
                g.write(stats.copy(badgeTiers = stats.badgeTiers + unlocked))
            }
            unlocked
        }

    /** Rakit profil pemain dari semua store (membaca disk — panggil di IO). */
    fun loadProfile(app: Context, gamification: GamificationStats): PlayerProfile {
        val surahs = runCatching { QuranRepository(app).surahList() }.getOrDefault(emptyList())
        return Achievements.profileOf(
            readingStats = ReadingStatsStore.fromContext(app).all(),
            vocab = VocabularyStatsStore.fromContext(app).read(),
            dream = DreamBigProgressStore.fromContext(app).read(),
            lughoh = LughohProgressStore.fromContext(app).read(),
            gamification = gamification,
            ayahCounts = surahs.associate { it.number to it.ayahCount },
        )
    }
}
