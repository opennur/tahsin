package com.tahsin.app.util

import com.tahsin.app.data.vocab.VocabState

/**
 * Satu badge progresif: metrik yang terus bertambah + ambang tier TAK
 * TERBATAS. Setelah tier pertama dibuka, masih ada tier kedua, ketiga, dst. —
 * user selalu punya target berikutnya untuk dikejar.
 *
 * `key` stabil: dipakai persistensi ([GamificationStats.badgeTiers]) dan
 * lookup teks terjemahan di UI (AppStrings: "badge_<key>_title"/"_desc").
 */
data class BadgeDef(
    val key: String,
    val emoji: String,
    /** Nilai progres saat ini (harus tumbuh tanpa batas). */
    val metric: (PlayerProfile) -> Int,
    /** Ambang metrik untuk membuka tier ke-[tier] (1-based); naik tak terbatas. */
    val tierThreshold: (tier: Int) -> Int,
)

/**
 * Progres satu badge: tier yang sudah diraih + jarak ke tier berikutnya.
 * currentTier = 0 berarti belum pernah dibuka (terkunci).
 */
data class BadgeProgress(
    val badge: BadgeDef,
    val currentTier: Int,
    val metricValue: Int,
    /** Ambang tier berikutnya (currentTier + 1). */
    val nextThreshold: Int,
) {
    val isUnlocked: Boolean get() = currentTier >= 1
    val fraction: Float
        get() = if (nextThreshold <= 0) 0f
        else (metricValue.toFloat() / nextThreshold).coerceIn(0f, 1f)
}

/**
 * Snapshot seluruh progres pemain yang dipakai menilai badge.
 * Di-rakit dari store masing-masing fitur lewat [Achievements.profileOf]
 * (murni — parameter di-inject supaya bisa diuji tanpa Android).
 */
data class PlayerProfile(
    val xp: Int = 0,
    val streak: Int = 0,
    /** Total percobaan bacaan Tahsin (hasil STT final). */
    val ayahAttempts: Int = 0,
    /** Jumlah ayat yang pernah dibaca dengan skor ≥ 90. */
    val perfectAyahs: Int = 0,
    /** Kartu kosakata yang dikuasai (correctCount > 0). */
    val wordsMastered: Int = 0,
    /** Jumlah ronde Dream BIG yang dimainkan. */
    val dreamBigRounds: Int = 0,
    val lughohRounds: Int = 0,
    /** Jumlah surah yang semua ayatnya pernah dibaca (attempts > 0). */
    val surahsCompleted: Int = 0,
)

/** Katalog badge progresif + evaluasi murni (tanpa Android — bisa diuji JVM). */
object Achievements {

    /**
     * Katalog lengkap badge. Semua metrik tumbuh tak terbatas, jadi tiap
     * badge punya tier tak terbatas (urutan menentukan urutan tampil).
     */
    val ALL: List<BadgeDef> = listOf(
        // XP: ambang mengikuti kurva level (100·t² = Lv 2, 3, 4, …).
        BadgeDef("xp", "🌟", metric = { it.xp }, tierThreshold = { tier -> 100 * tier * tier }),
        BadgeDef("streak", "🔥", metric = { it.streak }, tierThreshold = { tier -> 3 * tier }),
        BadgeDef("tahsin", "🎙️", metric = { it.ayahAttempts }, tierThreshold = { tier -> 10 * tier }),
        BadgeDef("tahsin-perfect", "💯", metric = { it.perfectAyahs }, tierThreshold = { tier -> tier }),
        BadgeDef("vocab", "🗝️", metric = { it.wordsMastered }, tierThreshold = { tier -> 50 * tier }),
        BadgeDef("dream", "🏆", metric = { it.dreamBigRounds }, tierThreshold = { tier -> 5 * tier }),
        BadgeDef("lughoh", "🗣️", metric = { it.lughohRounds }, tierThreshold = { tier -> 5 * tier }),
        BadgeDef("surah", "🕌", metric = { it.surahsCompleted }, tierThreshold = { tier -> tier }),
    )

    fun byKey(key: String): BadgeDef? = ALL.firstOrNull { it.key == key }

    /**
     * Pemetaan key badge era lama (13 badge one-time) → key baru (tier).
     * Dipakai migrasi sekali jalan di [GamificationStore.read].
     */
    val legacyKeyMap: Map<String, String> = mapOf(
        "first-step" to "xp",
        "level-2" to "xp",
        "level-5" to "xp",
        "streak-3" to "streak",
        "streak-7" to "streak",
        "tahsin-10" to "tahsin",
        "tahsin-perfect" to "tahsin-perfect",
        "vocab-50" to "vocab",
        "vocab-100" to "vocab",
        "dream-perfect" to "dream",
        "dream-10" to "dream",
        "lughoh-5" to "lughoh",
        "surah-complete" to "surah",
    )

    /**
     * Tier tertinggi yang bisa dibuka dengan nilai metrik [metricValue]
     * (tak terbatas: terus naik selama metrik melewati ambang tier).
     */
    fun highestReachableTier(badge: BadgeDef, metricValue: Int): Int {
        var tier = 1
        while (badge.tierThreshold(tier) <= metricValue) tier++
        return tier - 1
    }

    /** Progres satu badge: tier saat ini, nilai metrik, ambang tier berikutnya. */
    fun progressFor(badge: BadgeDef, profile: PlayerProfile, earnedTier: Int): BadgeProgress {
        val value = badge.metric(profile)
        return BadgeProgress(
            badge = badge,
            currentTier = earnedTier,
            metricValue = value,
            nextThreshold = badge.tierThreshold(earnedTier + 1),
        )
    }

    /**
     * Tier baru yang layak dibuka sekarang: key → tier, hanya yang LEBIH
     * TINGGI dari tier yang sudah diraih (earnedTiers). Dipanggil tiap event
     * XP; beberapa tier bisa terbuka sekaligus dalam satu evaluasi.
     */
    fun newlyUnlocked(profile: PlayerProfile, earnedTiers: Map<String, Int>): Map<String, Int> =
        ALL.mapNotNull { badge ->
            val reachable = highestReachableTier(badge, badge.metric(profile))
            val current = earnedTiers[badge.key] ?: 0
            if (reachable > current) badge.key to reachable else null
        }.toMap()

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
            ayahAttempts = attempted.sumOf { it.attempts },
            perfectAyahs = attempted.count { it.bestScore >= 90 },
            wordsMastered = vocab.cards.values.count { it.correctCount > 0 },
            dreamBigRounds = dream.roundsPlayed,
            lughohRounds = lughoh.roundsPlayed,
            surahsCompleted = surahsCompleted,
        )
    }
}
