package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.opennur.tahsin.data.quran.JuzStart
import org.opennur.tahsin.data.quran.MushafPage
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.PageSegment
import org.opennur.tahsin.data.quran.Surah

class ReadingProgressTest {

    private val pagination = MushafPagination(
        schemaVersion = 1,
        pageCount = 3,
        pages = listOf(
            MushafPage(1, listOf(PageSegment(1, 1, 7))),
            MushafPage(2, listOf(PageSegment(2, 1, 3))),
            MushafPage(3, listOf(PageSegment(2, 4, 5))),
        ),
        juzStarts = listOf(
            JuzStart(1, 1, 1),
            JuzStart(2, 99, 1),
        ),
    )

    private val surahs = listOf(
        Surah(1, "الفاتحة", "Al-Fatihah", 7),
        Surah(2, "البقرة", "Al-Baqarah", 5),
        Surah(3, "آل عمران", "Ali Imran", 0),
    )

    @Test
    fun `summary menghitung ayat surah juz halaman dan jatuh tempo`() {
        val stats = listOf(
            AyahStats(1, 1, attempts = 1, scoreSum = 80, bestScore = 80, reviewDueDay = 10),
            AyahStats(1, 2, attempts = 1, scoreSum = 50, bestScore = 50, reviewDueDay = 8),
            AyahStats(1, 3, attempts = 0, bestScore = 100),
            AyahStats(2, 1, attempts = 1, scoreSum = 70, bestScore = 70, reviewDueDay = 9),
        )

        val result = ReadingProgressEngine.summarize(stats, surahs, pagination, today = 10)

        assertThat(result.totalAyahs).isEqualTo(12)
        assertThat(result.practicedAyahs).isEqualTo(3)
        assertThat(result.goodAyahs).isEqualTo(2)
        assertThat(result.dueAyahs).isEqualTo(3)
        assertThat(result.startedSurahs).isEqualTo(2)
        assertThat(result.goodPages).isEqualTo(0)
        assertThat(result.reviewPages).isEqualTo(2)
        assertThat(result.untouchedPages).isEqualTo(1)
        assertThat(result.juz.first().practicedAyahs).isEqualTo(3)
        assertThat(result.surahs[0].goodAyahs).isEqualTo(1)
        assertThat(result.surahs[1].averageScore).isEqualTo(70)
        assertThat(result.practicedPercent).isEqualTo(25)
    }

    @Test
    fun `summary kosong memiliki cakupan nol`() {
        assertThat(ReadingProgressSummary.empty().practicedPercent).isEqualTo(0)
    }

    @Test
    fun `summary seluruh mushaf baik menghitung halaman dan juz baik`() {
        val stats = (1..7).map { ayah ->
            AyahStats(1, ayah, attempts = 1, scoreSum = 100, bestScore = 100, reviewDueDay = 20)
        } + (1..5).map { ayah ->
            AyahStats(2, ayah, attempts = 1, scoreSum = 100, bestScore = 100, reviewDueDay = 20)
        }

        val result = ReadingProgressEngine.summarize(stats, surahs, pagination, today = 10)

        assertThat(result.goodPages).isEqualTo(3)
        assertThat(result.goodJuz).isEqualTo(1)
        assertThat(result.dueAyahs).isEqualTo(0)
    }

    @Test
    fun `nextReviews diurutkan dari tanggal dan dibatasi`() {
        val stats = listOf(
            AyahStats(2, 1, reviewDueDay = 9),
            AyahStats(1, 2, reviewDueDay = 8),
            AyahStats(1, 1, reviewDueDay = 8),
            AyahStats(2, 2, reviewDueDay = 7),
            AyahStats(1, 3, reviewDueDay = 11),
            AyahStats(1, 4),
        )

        val result = ReadingProgressEngine.nextReviews(stats, today = 10, limit = 3)

        assertThat(result.map { it.surahNumber to it.ayahNumber })
            .containsExactly(2 to 2, 1 to 1, 1 to 2)
            .inOrder()
    }
}
