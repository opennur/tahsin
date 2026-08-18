package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Statistik kursus Shorof: skor terbaik, sesi, dan pelajaran selesai. */
data class ShorofStats(
    val bestScore: Int = 0,
    val sessionsPlayed: Int = 0,
    val completedLessonIds: Set<String> = emptySet(),
) {
    fun withSession(score: Int, lessonIds: Set<String>): ShorofStats = copy(
        bestScore = maxOf(bestScore, score),
        sessionsPlayed = sessionsPlayed + 1,
        completedLessonIds = completedLessonIds + lessonIds,
    )
}

/** Persistensi progres Shorof dengan pola Gson + filesDir. */
class ShorofProgressStore internal constructor(private val file: File) {
    companion object {
        fun fromContext(context: Context): ShorofProgressStore =
            ShorofProgressStore(File(context.applicationContext.filesDir, "shorof-progress.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<ShorofStats>() {}.type

    fun read(): ShorofStats = synchronized(this) {
        runCatching {
            if (!file.exists()) ShorofStats()
            else gson.fromJson<ShorofStats>(file.readText(), type) ?: ShorofStats()
        }.getOrDefault(ShorofStats())
    }

    fun write(stats: ShorofStats) {
        synchronized(this) {
            runCatching {
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(gson.toJson(stats))
                if (!temp.renameTo(file)) {
                    writeDirect(gson.toJson(stats))
                    temp.delete()
                }
            }
        }
    }

    internal fun writeDirect(json: String) {
        runCatching { file.writeText(json) }
    }
}
