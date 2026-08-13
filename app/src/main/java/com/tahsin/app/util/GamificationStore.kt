package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Ekonomi game: XP, level, streak harian, dan badge yang diraih.
 *
 * - [levelFor]/[xpForLevel]/[progressWithinLevel] murni & bisa di-unit-test.
 * - [withActivity] murni: catat aktivitas pada satu hari (epoch day) —
 *   hari sama → todayXp bertambah, streak tetap; hari berikutnya → streak +1;
 *   jeda >1 hari → streak reset ke 1.
 * - Persistensi mengikuti pola [DreamBigProgressStore] (Gson + filesDir,
 *   tulis atomik temp → rename).
 */
data class GamificationStats(
    val xp: Int = 0,
    /** XP yang dikumpulkan hari ini (direset otomatis saat tanggal berganti). */
    val todayXp: Int = 0,
    /** Hari epoch (LocalDate.toEpochDay) terakhir ada aktivitas; 0 = belum pernah. */
    val lastActiveDay: Long = 0L,
    /** Hari beruntun aktif (0 = belum ada). */
    val streak: Int = 0,
    /** ID badge yang sudah diraih (diisi oleh sistem achievement). */
    val badges: List<String> = emptyList(),
)

/** Hasil pencatatan satu aktivitas: keadaan sebelum/sesudah + deteksi naik level. */
data class ActivityResult(
    val before: GamificationStats,
    val after: GamificationStats,
) {
    val leveledUp: Boolean
        get() = Gamification.levelFor(after.xp) > Gamification.levelFor(before.xp)
}

/**
 * Logika murni ekonomi game — tanpa Android, bisa diuji unit.
 */
object Gamification {

    /** Jumlah XP per aktivitas (dipakai titik-titik hook di tiap fitur). */
    const val XP_AYAH_GOOD = 5     // bacaan Tahsin skor ≥ 70
    const val XP_AYAH_PERFECT = 10 // bacaan Tahsin skor ≥ 90
    const val XP_QUIZ_CORRECT = 2  // jawaban benar (Kuis Tajwid, Kosakata, dll.)
    const val XP_WORD_MASTERED = 10 // kata Kosakata baru dikuasai (correctCount 1)
    const val XP_DREAM_BIG_ROUND = 15 // ronde Dream BIG selesai
    const val XP_LUGHOH_SESSION = 10  // sesi Belajar Arab selesai

    /** Target XP harian (daily goal) — dicapai dengan aktivitas rutin. */
    const val DAILY_GOAL_XP = 50

    /**
     * Level dari total XP: level 1 pada 0 XP, naik tiap 100 × (level-1)².
     * level(L) = floor(sqrt(xp / 100)) + 1.
     */
    fun levelFor(xp: Int): Int {
        if (xp <= 0) return 1
        return floor(sqrt(xp / 100.0)).toInt() + 1
    }

    /** XP minimum untuk mencapai level [level] (1-based): 0, 100, 400, 900, ... */
    fun xpForLevel(level: Int): Int {
        require(level >= 1) { "Level harus ≥ 1: $level" }
        return (level - 1) * (level - 1) * 100
    }

    /** Progres 0.0–1.0 di dalam level saat ini (menuju level berikutnya). */
    fun progressWithinLevel(xp: Int): Float {
        val level = levelFor(xp)
        val base = xpForLevel(level)
        val next = xpForLevel(level + 1)
        if (next <= base) return 0f
        return ((xp - base).toFloat() / (next - base).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Catat aktivitas pada hari [today] (epoch day): update XP, todayXp,
     * dan streak. Hari sama → streak bertahan; besok → +1; jeda → reset 1.
     */
    fun withActivity(stats: GamificationStats, xpEarned: Int, today: Long): GamificationStats {
        require(xpEarned >= 0) { "XP tidak boleh negatif: $xpEarned" }
        val sameDay = stats.lastActiveDay == today
        val streak = when {
            sameDay -> stats.streak
            stats.lastActiveDay == 0L || today != stats.lastActiveDay + 1 -> 1
            else -> stats.streak + 1
        }
        val todayXp = if (sameDay) stats.todayXp + xpEarned else xpEarned
        return stats.copy(
            xp = stats.xp + xpEarned,
            todayXp = todayXp,
            lastActiveDay = today,
            streak = streak,
        )
    }

    /**
     * XP yang sah untuk ditampilkan "hari ini": 0 kalau hari terakhir aktif
     * bukan hari ini (nilai tersimpan milik kemarin — jangan tampil sebagai
     * progres hari ini). Murni untuk keperluan UI daily goal.
     */
    fun todayXpFor(stats: GamificationStats, today: Long): Int =
        if (stats.lastActiveDay == today) stats.todayXp else 0
}

/**
 * Penyimpanan persisten ekonomi game.
 * Format: satu file JSON di `filesDir/gamification.json` ([GamificationStats]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir).
 */
class GamificationStore internal constructor(private val file: File) {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "gamification.json"),
    )

    private val gson = Gson()
    private val statsType = object : TypeToken<GamificationStats>() {}.type

    /**
     * Kunci tulis GLOBAL (companion, bukan per-instance): award XP/cek badge
     * bisa datang dari beberapa ViewModel sekaligus — masing-masing membuat
     * instance store baru, jadi synchronized(this) tidak cukup. Semua
     * read-modify-write lewat kunci ini agar tidak ada kenaikan XP/streak
     * yang hilang atau rollback badge.
     */
    private companion object {
        val WRITE_LOCK = Any()
    }

    /** Jalankan [block] dengan kunci tulis global (RMW atomik lintas instance). */
    fun <T> withWriteLock(block: () -> T): T = synchronized(WRITE_LOCK, block)

    /** Keadaan saat ini; file rusak/kosong → default (semua 0). */
    fun read(): GamificationStats = synchronized(WRITE_LOCK) {
        runCatching {
            if (!file.exists()) GamificationStats()
            else gson.fromJson<GamificationStats>(file.readText(), statsType) ?: GamificationStats()
        }.getOrDefault(GamificationStats())
    }

    /** Simpan keadaan penuh (atomik: temp → rename, fallback tulis langsung). */
    fun write(stats: GamificationStats) {
        synchronized(WRITE_LOCK) {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(gson.toJson(stats))
                if (!tmp.renameTo(file)) {
                    // rename gagal — fallback tulis langsung; temp dibersihkan.
                    file.writeText(gson.toJson(stats))
                    tmp.delete()
                }
            }
        }
    }

    /**
     * Catat aktivitas XP hari ini lalu persist. Mengembalikan keadaan
     * sebelum/sesudah supaya pemanggil bisa deteksi level-up. Seluruh
     * read-modify-write di dalam SATU kunci global (aman dari race lintas
     * instance yang dipakai [com.tahsin.app.util.GamificationHub]).
     */
    fun recordActivity(xpEarned: Int, today: LocalDate = LocalDate.now()): ActivityResult =
        withWriteLock {
            val before = read()
            val after = Gamification.withActivity(before, xpEarned, today.toEpochDay())
            if (after != before) write(after)
            ActivityResult(before, after)
        }
}
