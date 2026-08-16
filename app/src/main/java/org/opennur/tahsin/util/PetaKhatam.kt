package org.opennur.tahsin.util

import org.opennur.tahsin.data.quran.MushafPagination

/** Status satu halaman atau juz di Peta Khatam. */
enum class KhatamStatus {
    /** Semua ayat di halaman/juz sudah dibaca dengan skor ≥ 70. */
    GOOD,
    /** Sebagian ayat sudah dibaca, atau skor belum mencapai 70. */
    NEEDS_REVIEW,
    /** Belum ada ayat yang dibaca. */
    UNTOUCHED,
}

/** Status satu halaman mushaf. */
data class PageStatusRow(val page: Int, val status: KhatamStatus)

/** Status satu juz. */
data class JuzStatusRow(val juz: Int, val status: KhatamStatus)

/** Ringkasan progres khatam. */
data class PetaKhatamSummary(
    val totalPages: Int,
    val goodPages: Int,
    val reviewPages: Int,
    val untouchedPages: Int,
) {
    val percentGood: Int
        get() = if (totalPages > 0) goodPages * 100 / totalPages else 0
}

/** Ambang skor "bagus" — konsisten dengan [Gamification.XP_AYAH_GOOD]. */
private const val GOOD_SCORE_THRESHOLD = 70

/** Logika murni Peta Khatam — tanpa Android, bisa di-unit-test. */
object PetaKhatamEngine {

    /**
     * Hitung status tiap halaman mushaf berdasarkan statistik bacaan.
     *
     * @param stats Daftar [AyahStats] (dari [ReadingStatsStore.all()])
     * @param pagination Paginasi mushaf (dari [org.opennur.tahsin.data.quran.QuranRepository.pagination])
     * @return Daftar [PageStatusRow] untuk 604 halaman
     */
    fun pageStatuses(
        stats: List<AyahStats>,
        pagination: MushafPagination,
    ): List<PageStatusRow> {
        // Kelompokkan statistik per halaman
        val pageStats = mutableMapOf<Int, MutableList<AyahStats>>()
        for (s in stats) {
            val page = pagination.pageOf(s.surahNumber, s.ayahNumber) ?: continue
            pageStats.getOrPut(page) { mutableListOf() }.add(s)
        }

        return (1..pagination.pageCount).map { page ->
            val pageData = pagination.pages.firstOrNull { it.page == page }
            val expectedAyahs = pageData?.segments?.sumOf { it.ayahCount } ?: 0
            val actual = pageStats[page].orEmpty()

            val status = when {
                expectedAyahs == 0 -> KhatamStatus.UNTOUCHED
                actual.isEmpty() -> KhatamStatus.UNTOUCHED
                actual.size < expectedAyahs -> KhatamStatus.NEEDS_REVIEW
                actual.all { it.attempts > 0 && it.bestScore >= GOOD_SCORE_THRESHOLD } -> KhatamStatus.GOOD
                else -> KhatamStatus.NEEDS_REVIEW
            }
            PageStatusRow(page = page, status = status)
        }
    }

    /**
     * Hitung status tiap juz (30 juz) berdasarkan statistik bacaan.
     *
     * @param stats Daftar [AyahStats]
     * @param pagination Paginasi mushaf
     * @return Daftar [JuzStatusRow] untuk 30 juz
     */
    fun juzStatuses(
        stats: List<AyahStats>,
        pagination: MushafPagination,
    ): List<JuzStatusRow> {
        // Kelompokkan per juz via halaman
        val juzStats = mutableMapOf<Int, MutableList<AyahStats>>()
        for (s in stats) {
            val page = pagination.pageOf(s.surahNumber, s.ayahNumber) ?: continue
            val juz = pagination.juzOfPage(page)
            juzStats.getOrPut(juz) { mutableListOf() }.add(s)
        }

        // Hitung jumlah ayat per juz dari pagination
        val juzAyahCounts = mutableMapOf<Int, Int>()
        for (page in pagination.pages) {
            val juz = pagination.juzOfPage(page.page)
            juzAyahCounts[juz] = (juzAyahCounts[juz] ?: 0) + page.segments.sumOf { it.ayahCount }
        }

        return (1..30).map { juz ->
            val expected = juzAyahCounts[juz] ?: 0
            val actual = juzStats[juz].orEmpty()

            val status = when {
                expected == 0 -> KhatamStatus.UNTOUCHED
                actual.isEmpty() -> KhatamStatus.UNTOUCHED
                actual.size < expected -> KhatamStatus.NEEDS_REVIEW
                actual.all { it.attempts > 0 && it.bestScore >= GOOD_SCORE_THRESHOLD } -> KhatamStatus.GOOD
                else -> KhatamStatus.NEEDS_REVIEW
            }
            JuzStatusRow(juz = juz, status = status)
        }
    }

    /**
     * Hitung ringkasan dari daftar status halaman.
     */
    fun summary(rows: List<PageStatusRow>): PetaKhatamSummary {
        val good = rows.count { it.status == KhatamStatus.GOOD }
        val review = rows.count { it.status == KhatamStatus.NEEDS_REVIEW }
        val untouched = rows.count { it.status == KhatamStatus.UNTOUCHED }
        return PetaKhatamSummary(
            totalPages = rows.size,
            goodPages = good,
            reviewPages = review,
            untouchedPages = untouched,
        )
    }
}
