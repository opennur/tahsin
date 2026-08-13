package com.tahsin.app.util

import com.tahsin.app.data.dreambig.DreamBigGame
import com.tahsin.app.data.vocab.VocabState

/**
 * Satu badge/achievement yang bisa diraih.
 *
 * `key` stabil: dipakai untuk persistensi ([GamificationStats.badges]) dan
 * lookup teks terjemahan di UI (AppStrings: "badge_<key>_title"/"_desc").
 * `emoji` + `unlocked` menetap di kode (bukan data) supaya evaluasi murni
 * dan bisa diuji unit.
 */
data class BadgeDef(
    val key: String,
    val emoji: String,
    val unlocked: (PlayerProfile) -> Boolean,
)

/**
 * Snapshot seluruh progres pemain yang dipakai menilai badge.
 * Di-rakit dari store masing-masing fitur lewat [Achievements.profileOf]
 * (murni — parameter di-inject supaya bisa diuji tanpa Android).
 */
data class PlayerProfile(
    val xp: Int = 0,
    val streak: Int = 0,
    val level: Int = 1,
    /** Total percobaan bacaan Tahsin (hasil STT final). */
    val ayahAttempts: Int = 0,
    /** Skor terbaik satu ayat (0..100). */
    val bestAyahScore: Int = 0,
    /** Kartu kosakata yang dikuasai (correctCount > 0). */
    val wordsMastered: Int = 0,
    /** Skor ronde terbaik Dream BIG (0..[DreamBigGame.QUESTIONS_PER_ROUND]). */
    val dreamBigBest: Int = 0,
    val dreamBigRounds: Int = 0,
    val lughohRounds: Int = 0,
    /** Jumlah surah yang semua ayatnya pernah dibaca (attempts > 0). */
    val surahsCompleted: Int = 0,
)

/** Katalog badge + evaluasi murni (tanpa Android — bisa diuji JVM). */
object Achievements {

    /** Katalog lengkap badge. Urutan menentukan urutan tampil. */
    val ALL: List<BadgeDef> = listOf(
        BadgeDef("first-step", "🌟") { it.xp > 0 },
        BadgeDef("streak-3", "🔥") { it.streak >= 3 },
        BadgeDef("streak-7", "⚡") { it.streak >= 7 },
        BadgeDef("level-2", "📚") { it.level >= 2 },
        BadgeDef("level-5", "🎓") { it.level >= 5 },
        BadgeDef("tahsin-10", "🎙️") { it.ayahAttempts >= 10 },
        BadgeDef("tahsin-perfect", "💯") { it.bestAyahScore >= 90 },
        BadgeDef("vocab-50", "🗝️") { it.wordsMastered >= 50 },
        BadgeDef("vocab-100", "📖") { it.wordsMastered >= 100 },
        BadgeDef("dream-perfect", "🏆") { it.dreamBigBest >= DreamBigGame.QUESTIONS_PER_ROUND },
        BadgeDef("dream-10", "🎮") { it.dreamBigRounds >= 10 },
        BadgeDef("lughoh-5", "🗣️") { it.lughohRounds >= 5 },
        BadgeDef("surah-complete", "🕌") { it.surahsCompleted >= 1 },
    )

    fun byKey(key: String): BadgeDef? = ALL.firstOrNull { it.key == key }

    /**
     * Badge yang kondisi unlock-nya sudah terpenuhi tapi belum pernah diraih
     * (urutan katalog). Dipakai tiap event XP: badge baru langsung diakui.
     */
    fun newlyEarned(profile: PlayerProfile, earnedKeys: Set<String>): List<BadgeDef> =
        ALL.filter { it.key !in earnedKeys && it.unlocked(profile) }

    /**
     * Rakit profil pemain dari state semua fitur. `ayahCounts`: nomor surah →
     * jumlah ayat (dari [com.tahsin.app.data.quran.QuranRepository.surahList])
     * — di-inject supaya bisa diuji; surah dianggap tuntas bila setiap
     * 1..ayahCount punya setidaknya satu percobaan bacaan.
     */
    fun profileOf(
        readingStats: List<AyahStats>,
        vocab: VocabState,
        dream: DreamBigStats,
        lughoh: LughohStats,
        gamification: GamificationStats,
        ayahCounts: Map<Int, Int>,
    ): PlayerProfile {
        val attempted = readingStats.filter { it.attempts > 0 }
        val attemptedBySurah = attempted
            .groupBy { it.surahNumber }
            .mapValues { (_, stats) -> stats.map { it.ayahNumber }.toSet() }
        val surahsCompleted = ayahCounts.count { (surah, count) ->
            val set = attemptedBySurah[surah].orEmpty()
            (1..count).all { it in set }
        }
        return PlayerProfile(
            xp = gamification.xp,
            streak = gamification.streak,
            level = Gamification.levelFor(gamification.xp),
            ayahAttempts = attempted.sumOf { it.attempts },
            bestAyahScore = attempted.maxOfOrNull { it.bestScore } ?: 0,
            wordsMastered = vocab.cards.values.count { it.correctCount > 0 },
            dreamBigBest = dream.bestScore,
            dreamBigRounds = dream.roundsPlayed,
            lughohRounds = lughoh.roundsPlayed,
            surahsCompleted = surahsCompleted,
        )
    }
}
