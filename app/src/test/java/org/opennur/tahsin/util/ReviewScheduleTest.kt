package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewScheduleTest {

    @Test
    fun `score rendah kembali besok dengan interval satu`() {
        val result = ReviewScheduleEngine.next(previousIntervalDays = 14, score = 69, today = 100)

        assertThat(result).isEqualTo(ReviewSchedule(dueDay = 101, intervalDays = 1))
    }

    @Test
    fun `bacaan baik pertama dijadwalkan besok`() {
        val result = ReviewScheduleEngine.next(previousIntervalDays = 0, score = 70, today = 100)

        assertThat(result).isEqualTo(ReviewSchedule(dueDay = 101, intervalDays = 1))
    }

    @Test
    fun `interval baik naik bertahap sampai tiga puluh hari`() {
        assertThat(ReviewScheduleEngine.next(1, 80, 100).intervalDays).isEqualTo(3)
        assertThat(ReviewScheduleEngine.next(3, 80, 100).intervalDays).isEqualTo(7)
        assertThat(ReviewScheduleEngine.next(7, 80, 100).intervalDays).isEqualTo(14)
        assertThat(ReviewScheduleEngine.next(14, 80, 100).intervalDays).isEqualTo(28)
        assertThat(ReviewScheduleEngine.next(28, 80, 100).intervalDays).isEqualTo(30)
    }
}
