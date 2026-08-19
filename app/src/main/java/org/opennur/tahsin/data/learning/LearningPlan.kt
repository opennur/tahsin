package org.opennur.tahsin.data.learning

/**
 * Tujuan utama yang dipilih pengguna saat pertama kali membuka aplikasi.
 * Tujuan ini menentukan urutan latihan harian, bukan membatasi fitur lain.
 */
enum class LearningGoal(val key: String) {
    RECITATION("recitation"),
    UNDERSTANDING("understanding"),
    MEMORIZATION("memorization"),
    ARABIC("arabic"),
    ;

    companion object {
        fun fromKey(key: String): LearningGoal =
            entries.firstOrNull { it.key == key } ?: RECITATION
    }
}

/** Satu jenis aktivitas yang bisa muncul dalam rencana harian. */
enum class LearningTaskType(val key: String) {
    RECITE("recite"),
    TAJWID("tajwid"),
    VOCABULARY("vocabulary"),
    UNDERSTAND("understand"),
    ARABIC("arabic"),
    NAHWU("nahwu"),
    SHOROF("shorof"),
    MEMORIZATION("memorization"),
    ;

    companion object {
        fun fromKey(key: String): LearningTaskType? = entries.firstOrNull { it.key == key }
    }
}

/** Aktivitas yang tampil pada satu hari. */
data class LearningTask(
    val type: LearningTaskType,
    val order: Int,
    val completed: Boolean,
)

/** Rencana belajar harian yang deterministik dan bisa dipulihkan setelah app dibuka ulang. */
data class DailyLearningPlan(
    val day: Long,
    val goal: LearningGoal,
    val tasks: List<LearningTask>,
) {
    val completedCount: Int
        get() = tasks.count { it.completed }

    val totalCount: Int
        get() = tasks.size

    val isComplete: Boolean
        get() = tasks.isNotEmpty() && completedCount == totalCount

    /** Langkah pertama yang belum selesai, dipakai sebagai CTA utama di Home. */
    val nextTask: LearningTask?
        get() = tasks.firstOrNull { !it.completed }
}

/** Logika murni penyusunan rencana; tidak bergantung Android atau penyimpanan. */
object LearningPlanEngine {

    /**
     * Urutan aktivitas agar setiap sesi menggabungkan baca, latihan, dan pemahaman.
     *
     * Default 60 menit mempertahankan rangkaian lengkap untuk pemanggil lama.
     * Pilihan 5/15 menit mengambil langkah awal saja agar target pendek tidak
     * tampil sebagai daftar tugas yang mustahil diselesaikan.
     */
    fun taskTypesFor(goal: LearningGoal, dailyMinutes: Int = 60): List<LearningTaskType> {
        val fullPlan = when (goal) {
            LearningGoal.RECITATION -> listOf(
                LearningTaskType.RECITE,
                LearningTaskType.TAJWID,
                LearningTaskType.VOCABULARY,
            )
            LearningGoal.UNDERSTANDING -> listOf(
                LearningTaskType.UNDERSTAND,
                LearningTaskType.VOCABULARY,
                LearningTaskType.RECITE,
            )
            LearningGoal.MEMORIZATION -> listOf(
                LearningTaskType.MEMORIZATION,
                LearningTaskType.RECITE,
                LearningTaskType.TAJWID,
            )
            LearningGoal.ARABIC -> listOf(
                LearningTaskType.ARABIC,
                LearningTaskType.NAHWU,
                LearningTaskType.SHOROF,
                LearningTaskType.VOCABULARY,
            )
        }
        val taskCount = when {
            dailyMinutes <= 5 -> 1
            dailyMinutes <= 15 -> 2
            else -> fullPlan.size
        }
        return fullPlan.take(taskCount)
    }

    fun build(
        day: Long,
        goal: LearningGoal,
        completedKeys: Set<String> = emptySet(),
        dailyMinutes: Int = 60,
    ): DailyLearningPlan = DailyLearningPlan(
        day = day,
        goal = goal,
        tasks = taskTypesFor(goal, dailyMinutes).mapIndexed { index, type ->
            LearningTask(
                type = type,
                order = index,
                completed = type.key in completedKeys,
            )
        },
    )
}
