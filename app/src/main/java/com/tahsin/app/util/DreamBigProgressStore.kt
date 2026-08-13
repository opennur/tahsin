package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tahsin.app.data.dreambig.DreamBigGame
import java.io.File

/** Progres game "Dream BIG": skor terbaik per level (day → skor). */
data class DreamBigProgress(
    /** day → skor terbaik ronde (0..10). Level ada di map = sudah pernah diselesaikan. */
    val bestScores: Map<Int, Int> = emptyMap(),
) {
    /** Hari yang sudah LULUS (skor ≥ [DreamBigGame.PASS_SCORE]) — dipakai untuk unlock. */
    val completedDays: Set<Int>
        get() = bestScores.filterValues { it >= DreamBigGame.PASS_SCORE }.keys

    /** Skor terbaik [day] (0 kalau belum pernah). */
    fun best(day: Int): Int = bestScores[day] ?: 0

    fun withBest(day: Int, score: Int): DreamBigProgress =
        copy(bestScores = bestScores + (day to maxOf(best(day), score)))
}

/**
 * Penyimpanan persisten progres game Dream BIG.
 * Format: satu file JSON di `filesDir/dreambig-progress.json` ([DreamBigProgress]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir, pola
 * [ReadingStatsStore]). Ditulis atomik (temp → rename) supaya tidak ada
 * file setengah jadi.
 */
class DreamBigProgressStore internal constructor(private val file: File) {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "dreambig-progress.json"),
    )

    private val gson = Gson()
    private val progressType = object : TypeToken<DreamBigProgress>() {}.type

    /** Keadaan saat ini; file rusak/kosong → default (progres kosong). */
    fun read(): DreamBigProgress = synchronized(this) {
        runCatching {
            if (!file.exists()) DreamBigProgress()
            else gson.fromJson<DreamBigProgress>(file.readText(), progressType) ?: DreamBigProgress()
        }.getOrDefault(DreamBigProgress())
    }

    /** Simpan keadaan penuh (atomik: temp → rename). */
    fun write(progress: DreamBigProgress) {
        synchronized(this) {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(gson.toJson(progress))
                if (!tmp.renameTo(file)) {
                    // rename gagal (mis. file tujuan ada di sistem lain) — fallback.
                    if (file.exists()) file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }
}
