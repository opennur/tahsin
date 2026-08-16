package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.opennur.tahsin.data.quran.JuzStart
import org.opennur.tahsin.data.quran.MushafPage
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.PageSegment

class PetaKhatamEngineTest {

    /** Pagination sederhana: 3 halaman, 1 juz, 2 surah. */
    private val pagination = MushafPagination(
        schemaVersion = 1,
        pageCount = 3,
        pages = listOf(
            MushafPage(1, listOf(PageSegment(1, 1, 7))),  // 7 ayat
            MushafPage(2, listOf(PageSegment(2, 1, 3))),  // 3 ayat
            MushafPage(3, listOf(PageSegment(2, 4, 5))),  // 2 ayat
        ),
        juzStarts = listOf(JuzStart(1, 1, 1)),
    )

    // ---- pageStatuses ----

    @Test
    fun pageStatuses_emptyStats_allUntouched() {
        val result = PetaKhatamEngine.pageStatuses(emptyList(), pagination)
        assertThat(result).hasSize(3)
        assertThat(result.all { it.status == KhatamStatus.UNTOUCHED }).isTrue()
    }

    @Test
    fun pageStatuses_allGood() {
        val stats = (1..7).map { AyahStats(1, it, attempts = 3, bestScore = 80) } +
            (1..5).map { AyahStats(2, it, attempts = 1, bestScore = 70) }
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.GOOD)   // page 1
        assertThat(result[1].status).isEqualTo(KhatamStatus.GOOD)   // page 2
        assertThat(result[2].status).isEqualTo(KhatamStatus.GOOD)   // page 3
    }

    @Test
    fun pageStatuses_partialCoverage_review() {
        // Hanya 5 dari 7 ayat di halaman 1
        val stats = (1..5).map { AyahStats(1, it, attempts = 1, bestScore = 80) }
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.NEEDS_REVIEW)
        assertThat(result[1].status).isEqualTo(KhatamStatus.UNTOUCHED)
        assertThat(result[2].status).isEqualTo(KhatamStatus.UNTOUCHED)
    }

    @Test
    fun pageStatuses_allPracticed_lowScore_review() {
        // Semua ayat dibaca tapi skor < 70
        val stats = (1..7).map { AyahStats(1, it, attempts = 1, bestScore = 50) }
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.NEEDS_REVIEW)
    }

    @Test
    fun pageStatuses_goodThresholdExactly70_good() {
        val stats = (1..7).map { AyahStats(1, it, attempts = 1, bestScore = 70) }
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.GOOD)
    }

    @Test
    fun pageStatuses_belowThreshold_review() {
        val stats = (1..7).map { AyahStats(1, it, attempts = 1, bestScore = 69) }
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.NEEDS_REVIEW)
    }

    @Test
    fun pageStatuses_mixedPages() {
        // Halaman 1: semua good
        val stats = (1..7).map { AyahStats(1, it, attempts = 2, bestScore = 90) } +
            // Halaman 2: sebagian
            listOf(AyahStats(2, 1, attempts = 1, bestScore = 80)) +
            // Halaman 3: belum
            emptyList()
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.GOOD)
        assertThat(result[1].status).isEqualTo(KhatamStatus.NEEDS_REVIEW)
        assertThat(result[2].status).isEqualTo(KhatamStatus.UNTOUCHED)
    }

    // ---- juzStatuses ----

    @Test
    fun juzStatuses_emptyStats_untouched() {
        val result = PetaKhatamEngine.juzStatuses(emptyList(), pagination)
        assertThat(result).hasSize(1) // 1 juz in test pagination
        assertThat(result[0].status).isEqualTo(KhatamStatus.UNTOUCHED)
    }

    @Test
    fun juzStatuses_allGood() {
        val stats = (1..7).map { AyahStats(1, it, attempts = 1, bestScore = 80) } +
            (1..5).map { AyahStats(2, it, attempts = 1, bestScore = 80) }
        val result = PetaKhatamEngine.juzStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.GOOD)
    }

    @Test
    fun juzStatuses_partialCoverage_review() {
        val stats = (1..7).map { AyahStats(1, it, attempts = 1, bestScore = 80) }
        val result = PetaKhatamEngine.juzStatuses(stats, pagination)
        assertThat(result[0].status).isEqualTo(KhatamStatus.NEEDS_REVIEW)
    }

    // ---- summary ----

    @Test
    fun summary_empty() {
        val result = PetaKhatamEngine.summary(emptyList())
        assertThat(result.totalPages).isEqualTo(0)
        assertThat(result.percentGood).isEqualTo(0)
    }

    @Test
    fun summary_allGood() {
        val rows = (1..604).map { PageStatusRow(it, KhatamStatus.GOOD) }
        val result = PetaKhatamEngine.summary(rows)
        assertThat(result.totalPages).isEqualTo(604)
        assertThat(result.goodPages).isEqualTo(604)
        assertThat(result.reviewPages).isEqualTo(0)
        assertThat(result.untouchedPages).isEqualTo(0)
        assertThat(result.percentGood).isEqualTo(100)
    }

    @Test
    fun summary_mixed() {
        val rows = (1..100).map { PageStatusRow(it, KhatamStatus.GOOD) } +
            (101..200).map { PageStatusRow(it, KhatamStatus.NEEDS_REVIEW) } +
            (201..604).map { PageStatusRow(it, KhatamStatus.UNTOUCHED) }
        val result = PetaKhatamEngine.summary(rows)
        assertThat(result.totalPages).isEqualTo(604)
        assertThat(result.goodPages).isEqualTo(100)
        assertThat(result.reviewPages).isEqualTo(100)
        assertThat(result.untouchedPages).isEqualTo(404)
        assertThat(result.percentGood).isEqualTo(16)  // 100*100/604 = 16
    }

    @Test
    fun summary_percentRoundsDown() {
        val rows = listOf(
            PageStatusRow(1, KhatamStatus.GOOD),
            PageStatusRow(2, KhatamStatus.NEEDS_REVIEW),
            PageStatusRow(3, KhatamStatus.UNTOUCHED),
        )
        val result = PetaKhatamEngine.summary(rows)
        assertThat(result.percentGood).isEqualTo(33)  // 100*1/3 = 33
    }

    // ---- edge cases ----

    @Test
    fun pageStatuses_statsOutsideMushaf_ignored() {
        val stats = listOf(
            AyahStats(999, 1, attempts = 1, bestScore = 80), // surah 999 doesn't exist
            AyahStats(1, 999, attempts = 1, bestScore = 80), // ayah 999 doesn't exist
        )
        val result = PetaKhatamEngine.pageStatuses(stats, pagination)
        assertThat(result.all { it.status == KhatamStatus.UNTOUCHED }).isTrue()
    }
}
