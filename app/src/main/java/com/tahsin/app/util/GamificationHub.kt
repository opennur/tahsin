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
        val g = GamificationStore(app)
        val result = g.recordActivity(xp)
        val newBadges = checkAndUnlock(app, g)
        celebrate(result, newBadges)
        return result
    }

    /** Tingkat streak yang dirayakan (semakin tinggi semakin langka). */
    private val STREAK_MILESTONES = setOf(3, 7, 14, 30)

    /**
     * Post satu perayaan paling penting: level-up > streak milestone > badge.
     * Satu event per pemanggilan agar dialog tidak menumpuk.
     */
    private fun celebrate(result: ActivityResult, newBadges: List<BadgeDef>) {
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
        if (newBadges.isNotEmpty()) {
            GamificationEvents.post(
                CelebrationEvent(
                    type = CelebrationType.BADGE_EARNED,
                    badgeKey = newBadges.first().key,
                ),
            )
        }
    }

    /**
     * Evaluasi semua badge terhadap progres saat ini; badge yang baru
     * terpenuhi ditambahkan ke [GamificationStats.badges] dan di-persist.
     * Mengembalikan daftar badge yang baru diraih (untuk notifikasi UI).
     */
    fun checkAndUnlock(app: Context, g: GamificationStore = GamificationStore(app)): List<BadgeDef> =
        g.withWriteLock {
            val stats = g.read()
            val profile = loadProfile(app, stats)
            val newOnes = Achievements.newlyEarned(profile, stats.badges.toSet())
            if (newOnes.isNotEmpty()) {
                g.write(stats.copy(badges = (stats.badges + newOnes.map { it.key }).distinct()))
            }
            newOnes
        }

    /** Rakit profil pemain dari semua store (membaca disk — panggil di IO). */
    fun loadProfile(app: Context, gamification: GamificationStats): PlayerProfile {
        val surahs = runCatching { QuranRepository(app).surahList() }.getOrDefault(emptyList())
        return Achievements.profileOf(
            readingStats = ReadingStatsStore(app).all(),
            vocab = VocabularyStatsStore(app).read(),
            dream = DreamBigProgressStore(app).read(),
            lughoh = LughohProgressStore(app).read(),
            gamification = gamification,
            ayahCounts = surahs.associate { it.number to it.ayahCount },
        )
    }
}
