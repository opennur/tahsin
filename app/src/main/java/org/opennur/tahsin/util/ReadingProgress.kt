package org.opennur.tahsin.util

import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.Surah

/** Rincian latihan satu surah untuk layar progress. */
data class SurahProgressRow(
    val number: Int,
    val name: String,
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
    val averageScore: Int,
    val dueAyahs: Int,
)

/** Rincian latihan satu juz berdasarkan metadata paginasi Madani. */
data class JuzProgressRow(
    val juz: Int,
    val startPage: Int,
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
)

/** Ringkasan yang dipakai bersama oleh Beranda dan Statistik. */
data class ReadingProgressSummary(
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
    val dueAyahs: Int,
    val startedSurahs: Int,
    val goodPages: Int,
    val reviewPages: Int,
    val untouchedPages: Int,
    val goodJuz: Int,
    val surahs: List<SurahProgressRow>,
    val juz: List<JuzProgressRow>,
) {
    val practicedPercent: Int
        get() = if (totalAyahs == 0) 0 else practicedAyahs * 100 / totalAyahs

    companion object {
        fun empty() = ReadingProgressSummary(
            totalAyahs = 0,
            practicedAyahs = 0,
            goodAyahs = 0,
            dueAyahs = 0,
            startedSurahs = 0,
            goodPages = 0,
            reviewPages = 0,
            untouchedPages = 0,
            goodJuz = 0,
            surahs = emptyList(),
            juz = emptyList(),
        )
    }
}

/** Agregasi progres mushaf dari statistik bacaan per ayat. */
object ReadingProgressEngine {

    fun summarize(
        stats: List<AyahStats>,
        surahs: List<Surah>,
        pagination: MushafPagination,
        today: Long,
    ): ReadingProgressSummary {
        val byAyah = stats.associateBy { it.surahNumber to it.ayahNumber }
        val pages = PetaKhatamEngine.pageStatuses(stats, pagination)
        val juzRows = juzProgress(byAyah, pagination)
        val surahRows = surahProgress(byAyah, surahs, today)

        return ReadingProgressSummary(
            totalAyahs = surahs.sumOf { it.ayahCount },
            practicedAyahs = stats.count { it.attempts > 0 },
            goodAyahs = stats.count { it.attempts > 0 && it.bestScore >= ReviewScheduleEngine.GOOD_SCORE_THRESHOLD },
            dueAyahs = stats.count { it.reviewDueDay in 1..today },
            startedSurahs = surahRows.count { it.practicedAyahs > 0 },
            goodPages = pages.count { it.status == KhatamStatus.GOOD },
            reviewPages = pages.count { it.status == KhatamStatus.NEEDS_REVIEW },
            untouchedPages = pages.count { it.status == KhatamStatus.UNTOUCHED },
            goodJuz = juzRows.count { it.totalAyahs > 0 && it.goodAyahs == it.totalAyahs },
            surahs = surahRows,
            juz = juzRows,
        )
    }

    fun nextReviews(stats: List<AyahStats>, today: Long, limit: Int = 5): List<AyahStats> =
        stats.filter { it.reviewDueDay in 1..today }
            .sortedWith(compareBy<AyahStats> { it.reviewDueDay }.thenBy { it.surahNumber }.thenBy { it.ayahNumber })
            .take(limit)

    private fun surahProgress(
        byAyah: Map<Pair<Int, Int>, AyahStats>,
        surahs: List<Surah>,
        today: Long,
    ): List<SurahProgressRow> = surahs.map { surah ->
        val entries = (1..surah.ayahCount).mapNotNull { ayah -> byAyah[surah.number to ayah] }
        val practiced = entries.count { it.attempts > 0 }
        val good = entries.count { it.attempts > 0 && it.bestScore >= ReviewScheduleEngine.GOOD_SCORE_THRESHOLD }
        SurahProgressRow(
            number = surah.number,
            name = surah.nameLatin,
            totalAyahs = surah.ayahCount,
            practicedAyahs = practiced,
            goodAyahs = good,
            averageScore = entries.filter { it.attempts > 0 }.map { it.avgScore }.averageOrZero(),
            dueAyahs = entries.count { it.reviewDueDay in 1..today },
        )
    }

    private fun juzProgress(
        byAyah: Map<Pair<Int, Int>, AyahStats>,
        pagination: MushafPagination,
    ): List<JuzProgressRow> = (1..30).map { juz ->
        val pages = pagination.pages.filter { pagination.juzOfPage(it.page) == juz }
        val coordinates = pages.flatMap { page ->
            page.segments.flatMap { segment ->
                (segment.fromAyah..segment.toAyah).map { segment.surah to it }
            }
        }.toSet()
        val values = coordinates.mapNotNull { byAyah[it] }
        JuzProgressRow(
            juz = juz,
            startPage = pagination.juzStarts.firstOrNull { it.juz == juz }
                ?.let { pagination.pageOf(it.surah, it.ayah) ?: 1 } ?: 1,
            totalAyahs = coordinates.size,
            practicedAyahs = values.count { it.attempts > 0 },
            goodAyahs = values.count { it.attempts > 0 && it.bestScore >= ReviewScheduleEngine.GOOD_SCORE_THRESHOLD },
        )
    }

    private fun List<Int>.averageOrZero(): Int = if (isEmpty()) 0 else average().toInt()
}
