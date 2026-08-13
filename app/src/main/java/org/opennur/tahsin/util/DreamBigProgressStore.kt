package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Statistik mode arcade "Dream BIG": rekor & jumlah ronde yang dimainkan. */
data class DreamBigStats(
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val roundsPlayed: Int = 0,
) {
    /** Gabungkan hasil satu ronde (skor & streak terbaik, ronde +1). */
    fun withRound(score: Int, streak: Int): DreamBigStats = copy(
        bestScore = maxOf(bestScore, score),
        bestStreak = maxOf(bestStreak, streak),
        roundsPlayed = roundsPlayed + 1,
    )
}

/**
 * Penyimpanan persisten statistik game Dream BIG (arcade).
 * Format: satu file JSON di `filesDir/dreambig-progress.json` ([DreamBigStats]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir, pola
 * [ReadingStatsStore]). Ditulis atomik (temp → rename). File lama berisi
 * `bestScores` (era level) dibaca sebagai default (field tak dikenal diabaikan).
 */
class DreamBigProgressStore internal constructor(private val file: File) {

    companion object {
        /** Factory Android: file di filesDir — ctor tetap murni (File) agar bisa dicakup 100%. */
        fun fromContext(context: Context): DreamBigProgressStore =
            DreamBigProgressStore(File(context.applicationContext.filesDir, "dreambig-progress.json"))
    }

    private val gson = Gson()
    private val statsType = object : TypeToken<DreamBigStats>() {}.type

    /** Keadaan saat ini; file rusak/kosong → default (semua 0). */
    fun read(): DreamBigStats = synchronized(this) {
        runCatching {
            if (!file.exists()) DreamBigStats()
            else gson.fromJson<DreamBigStats>(file.readText(), statsType) ?: DreamBigStats()
        }.getOrDefault(DreamBigStats())
    }

    /** Simpan keadaan penuh (atomik: temp → rename, fallback tulis langsung). */
    fun write(stats: DreamBigStats) {
        synchronized(this) {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(gson.toJson(stats))
                if (!tmp.renameTo(file)) {
                    // rename gagal (mis. file tujuan ada di sistem lain) — fallback
                    // tulis langsung; file temp dibersihkan supaya tidak nyangkut.
                    writeDirect(gson.toJson(stats))
                    tmp.delete()
                }
            }
        }
    }

    /**
     * Fallback tulis langsung — dipisah supaya jalur sukses bisa diuji unit
     * (file biasa). Kegagalan (mis. target direktori) ditelan runCatching.
     */
    internal fun writeDirect(json: String) {
        runCatching { file.writeText(json) }
    }
}
