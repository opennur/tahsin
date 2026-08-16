package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Persisted completion state for the current daily learning plan. */
data class LearningPlanSnapshot(
    val day: Long = Long.MIN_VALUE,
    val goalKey: String = "recitation",
    val completedKeys: Set<String> = emptySet(),
)

/**
 * Small file-backed store for daily plan state. It deliberately does not store
 * Qur'an content; only the user's selected goal and completed task keys.
 */
class LearningPlanStore internal constructor(private val file: File) {

    companion object {
        fun fromContext(context: Context): LearningPlanStore =
            LearningPlanStore(File(context.applicationContext.filesDir, "learning-plan.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<LearningPlanSnapshot>() {}.type

    fun read(): LearningPlanSnapshot = synchronized(this) {
        runCatching {
            if (!file.exists()) return@synchronized LearningPlanSnapshot()
            gson.fromJson<LearningPlanSnapshot>(file.readText(), type) ?: LearningPlanSnapshot()
        }.getOrDefault(LearningPlanSnapshot())
    }

    fun markComplete(day: Long, goalKey: String, taskKey: String): LearningPlanSnapshot =
        synchronized(this) {
            val current = read()
            val base = if (current.day == day && current.goalKey == goalKey) {
                current
            } else {
                LearningPlanSnapshot(day = day, goalKey = goalKey)
            }
            val updated = base.copy(completedKeys = base.completedKeys + taskKey)
            write(updated)
            updated
        }

    private fun write(snapshot: LearningPlanSnapshot) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.writeText(gson.toJson(snapshot))
            if (!temporary.renameTo(file)) {
                file.writeText(gson.toJson(snapshot))
                temporary.delete()
            }
        }
    }
}
