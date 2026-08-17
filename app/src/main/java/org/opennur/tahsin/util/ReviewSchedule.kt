package org.opennur.tahsin.util

/** Jadwal murajaah Tahsin yang disimpan terpisah dari antrean hafalan. */
data class ReviewSchedule(
    val dueDay: Long,
    val intervalDays: Int,
)

/** Aturan murni penjadwalan ulang hasil bacaan. */
object ReviewScheduleEngine {

    const val GOOD_SCORE_THRESHOLD = 70

    /**
     * Skor di bawah 70 kembali ke latihan besok. Skor baik mengikuti
     * interval bertahap 1, 3, 7, 14, lalu maksimal 30 hari.
     */
    fun next(previousIntervalDays: Int, score: Int, today: Long): ReviewSchedule {
        if (score < GOOD_SCORE_THRESHOLD) {
            return ReviewSchedule(dueDay = today + 1, intervalDays = 1)
        }

        val interval = when {
            previousIntervalDays <= 0 -> 1
            previousIntervalDays < 3 -> 3
            previousIntervalDays < 7 -> 7
            previousIntervalDays < 14 -> 14
            else -> (previousIntervalDays * 2).coerceAtMost(30)
        }
        return ReviewSchedule(dueDay = today + interval, intervalDays = interval)
    }
}
