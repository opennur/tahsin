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
}

/** Logika murni penyusunan rencana; tidak bergantung Android atau penyimpanan. */
object LearningPlanEngine {

    /** Urutan aktivitas pendek agar setiap sesi menggabungkan baca, latihan, dan pemahaman. */
    fun taskTypesFor(goal: LearningGoal): List<LearningTaskType> = when (goal) {
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
            LearningTaskType.VOCABULARY,
        )
    }

    fun build(
        day: Long,
        goal: LearningGoal,
        completedKeys: Set<String> = emptySet(),
    ): DailyLearningPlan = DailyLearningPlan(
        day = day,
        goal = goal,
        tasks = taskTypesFor(goal).mapIndexed { index, type ->
            LearningTask(
                type = type,
                order = index,
                completed = type.key in completedKeys,
            )
        },
    )
}
