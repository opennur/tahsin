package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Statistik kursus Nahwu: skor terbaik, sesi, dan pelajaran yang selesai. */
data class NahwuStats(
    val bestScore: Int = 0,
    val sessionsPlayed: Int = 0,
    val completedLessonIds: Set<String> = emptySet(),
) {
    fun withSession(score: Int, lessonIds: Set<String>): NahwuStats = copy(
        bestScore = maxOf(bestScore, score),
        sessionsPlayed = sessionsPlayed + 1,
        completedLessonIds = completedLessonIds + lessonIds,
    )
}

/** Persistensi progres Nahwu dengan pola Gson + filesDir yang dipakai proyek. */
class NahwuProgressStore internal constructor(private val file: File) {
    companion object {
        fun fromContext(context: Context): NahwuProgressStore =
            NahwuProgressStore(File(context.applicationContext.filesDir, "nahwu-progress.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<NahwuStats>() {}.type

    fun read(): NahwuStats = synchronized(this) {
        runCatching {
            if (!file.exists()) NahwuStats()
            else gson.fromJson<NahwuStats>(file.readText(), type) ?: NahwuStats()
        }.getOrDefault(NahwuStats())
    }

    fun write(stats: NahwuStats) {
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
