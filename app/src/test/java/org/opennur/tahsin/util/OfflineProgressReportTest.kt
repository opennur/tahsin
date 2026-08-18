package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OfflineProgressReportTest {

    @Test
    fun `encoder produces readable anonymous json`() {
        val json = OfflineProgressReportEncoder.encode(
            OfflineProgressReport(
                generatedAt = 10L,
                totalAyahs = 10,
                practicedAyahs = 4,
                goodAyahs = 3,
                dueAyahs = 1,
                goodPages = 2,
                reviewPages = 1,
                untouchedPages = 1,
                goodJuz = 0,
                totalSessions = 5,
                bestScorePct = 90,
                streak = 2,
                xp = 20,
                surahs = emptyList(),
                juz = emptyList(),
            ),
        )

        assertThat(json).contains("\"schemaVersion\": 1")
        assertThat(json).contains("\"practicedAyahs\": 4")
        assertThat(json).doesNotContain("email")
        assertThat(json).doesNotContain("transcript")
    }
}
