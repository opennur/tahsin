package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Progres fitur "Belajar Arab": id pelajaran yang sudah dituntaskan. */
data class LughohProgress(
    val completedLessonIds: Set<String> = emptySet(),
) {
    fun isCompleted(lessonId: String): Boolean = lessonId in completedLessonIds

    fun withCompleted(lessonId: String): LughohProgress =
        copy(completedLessonIds = completedLessonIds + lessonId)
}

/**
 * Penyimpanan persisten progres Belajar Arab.
 * Format: satu file JSON di `filesDir/lughoh-progress.json` ([LughohProgress]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir, pola
 * [DreamBigProgressStore]). Ditulis atomik (temp → rename).
 */
class LughohProgressStore internal constructor(private val file: File) {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, "lughoh-progress.json"),
    )

    private val gson = Gson()
    private val progressType = object : TypeToken<LughohProgress>() {}.type

    /** Keadaan saat ini; file rusak/kosong → default (progres kosong). */
    fun read(): LughohProgress = synchronized(this) {
        runCatching {
            if (!file.exists()) LughohProgress()
            else gson.fromJson<LughohProgress>(file.readText(), progressType) ?: LughohProgress()
        }.getOrDefault(LughohProgress())
    }

    /** Simpan keadaan penuh (atomik: temp → rename). */
    fun write(progress: LughohProgress) {
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
