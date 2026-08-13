package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Statistik mode arcade "Belajar Arab": rekor & jumlah sesi latihan acak. */
data class LughohStats(
    val bestScore: Int = 0,
    val roundsPlayed: Int = 0,
) {
    /** Gabungkan hasil satu sesi latihan (skor terbaik, sesi +1). */
    fun withRound(score: Int): LughohStats = copy(
        bestScore = maxOf(bestScore, score),
        roundsPlayed = roundsPlayed + 1,
    )
}

/**
 * Penyimpanan persisten statistik Belajar Arab (arcade).
 * Format: satu file JSON di `filesDir/lughoh-progress.json` ([LughohStats]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir, pola
 * [DreamBigProgressStore]). Ditulis atomik (temp → rename). File lama berisi
 * `completedLessonIds` (era pelajaran selesai) dibaca sebagai default.
 */
class LughohProgressStore internal constructor(private val file: File) {

    companion object {
        /** Factory Android: file di filesDir — ctor tetap murni (File) agar bisa dicakup 100%. */
        fun fromContext(context: Context): LughohProgressStore =
            LughohProgressStore(File(context.applicationContext.filesDir, "lughoh-progress.json"))
    }

    private val gson = Gson()
    private val statsType = object : TypeToken<LughohStats>() {}.type

    /** Keadaan saat ini; file rusak/kosong → default (semua 0). */
    fun read(): LughohStats = synchronized(this) {
        runCatching {
            if (!file.exists()) LughohStats()
            else gson.fromJson<LughohStats>(file.readText(), statsType) ?: LughohStats()
        }.getOrDefault(LughohStats())
    }

    /** Simpan keadaan penuh (atomik: temp → rename, fallback tulis langsung). */
    fun write(stats: LughohStats) {
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
