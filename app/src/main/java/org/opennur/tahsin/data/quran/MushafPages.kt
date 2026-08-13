package org.opennur.tahsin.data.quran

import com.google.gson.Gson

/**
 * Paginasi mushaf Madani (604 halaman, riwayat Hafs) — hasil
 * `tools/build_pages.py` → `assets/quran/pages.json`.
 *
 * Model murni (tanpa Context) supaya bisa di-unit-test. I/O aset ditangani
 * [QuranRepository]; parser ini hanya JSON → model + lookup.
 */
data class MushafPagination(
    val schemaVersion: Int,
    val pageCount: Int,
    val pages: List<MushafPage>,
    val juzStarts: List<JuzStart>,
) {
    /** Ayat pertama yang muncul di halaman [page] (untuk judul/nomor juz). */
    fun firstAyahOf(page: Int): Pair<Int, Int>? {
        val p = pages.firstOrNull { it.page == page } ?: return null
        return p.firstAyahOfPage()
    }

    /** Halaman yang memuat surah:ayat (null kalau di luar mushaf). */
    fun pageOf(surah: Int, ayah: Int): Int? {
        // Pencarian biner: halaman berurutan & segmen naik terhadap (surah, ayat).
        var lo = 0
        var hi = pages.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compareAyahToPage(surah, ayah, mid)
            when {
                cmp == 0 -> return pages[mid].page
                cmp < 0 -> hi = mid - 1
                else -> lo = mid + 1
            }
        }
        return null
    }

    /** Semua halaman yang memuat surah [surah]. */
    fun pagesOf(surah: Int): List<Int> =
        pages.filter { p -> p.segments.any { it.surah == surah } }.map { it.page }

    /** Halaman pertama yang memuat surah [surah]. */
    fun firstPageOf(surah: Int): Int? = pagesOf(surah).firstOrNull()

    /** Nomor juz yang aktif di awal halaman [page]. */
    fun juzOfPage(page: Int): Int {
        val first = firstAyahOf(page) ?: return juzStarts.firstOrNull()?.juz ?: 1
        var juz = juzStarts.firstOrNull()?.juz ?: 1
        for (js in juzStarts) {
            val (jsSurah, jsAyah) = js.surah to js.ayah
            if (jsSurah < first.first || (jsSurah == first.first && jsAyah <= first.second)) {
                juz = js.juz
            }
        }
        return juz
    }

    /** Juz yang dimulai tepat di halaman [page] (null kalau tidak ada). */
    fun juzStartingOnPage(page: Int): JuzStart? {
        val first = firstAyahOf(page) ?: return null
        return juzStarts.firstOrNull { it.surah == first.first && it.ayah == first.second }
    }

    /**
     * -1 jika (surah, ayah) sebelum halaman [index], 0 jika ada di halaman itu,
     * +1 jika sesudahnya. Membandingkan dengan ayat PERTAMA halaman berikutnya.
     */
    private fun compareAyahToPage(surah: Int, ayah: Int, index: Int): Int {
        val page = pages[index]
        val inPage = page.segments.any { s ->
            s.surah == surah && ayah >= s.fromAyah && ayah <= s.toAyah
        }
        if (inPage) return 0
        val firstNext = pages.getOrNull(index + 1)?.firstAyahOfPage() ?: return -1
        return if (surah < firstNext.first || (surah == firstNext.first && ayah < firstNext.second)) -1 else 1
    }

    private fun MushafPage.firstAyahOfPage(): Pair<Int, Int>? {
        val seg = segments.firstOrNull() ?: return null
        return seg.surah to seg.fromAyah
    }
}

/** Satu halaman mushaf: daftar segmen surah yang tampil di halaman itu. */
data class MushafPage(
    val page: Int,
    val segments: List<PageSegment>,
)

/** Rentang ayat satu surah di dalam satu halaman (bersambung). */
data class PageSegment(
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
) {
    val ayahCount: Int get() = toAyah - fromAyah + 1
}

/** Awal sebuah juz (juz [juz] dimulai di surah:ayah). */
data class JuzStart(
    val juz: Int,
    val surah: Int,
    val ayah: Int,
)

/** Parsing `pages.json` — JSON rusak/kosong → paginasi kosong (tidak crash). */
object MushafPagesParser {

    private val gson = Gson()

    fun parse(json: String): MushafPagination {
        val parsed = runCatching { gson.fromJson(json, PagesJson::class.java) }.getOrNull()
            ?: return MushafPagination(0, 0, emptyList(), emptyList())
        return MushafPagination(
            schemaVersion = parsed.schemaVersion ?: 0,
            pageCount = parsed.pageCount ?: 0,
            pages = parsed.pages.orEmpty().map { it.toPage() },
            juzStarts = parsed.juzStarts.orEmpty().map { it.toJuzStart() },
        )
    }

    private data class PagesJson(
        val schemaVersion: Int? = null,
        val pageCount: Int? = null,
        val pages: List<PageJson>? = null,
        val juzStarts: List<JuzStartJson>? = null,
    )

    private data class PageJson(
        val page: Int = 0,
        val segments: List<SegmentJson>? = null,
    ) {
        fun toPage() = MushafPage(
            page = page,
            segments = segments.orEmpty().map { it.toSegment() },
        )
    }

    private data class SegmentJson(
        val surah: Int = 0,
        val from: Int = 0,
        val to: Int = 0,
    ) {
        fun toSegment() = PageSegment(surah = surah, fromAyah = from, toAyah = to)
    }

    private data class JuzStartJson(
        val juz: Int = 0,
        val surah: Int = 0,
        val ayah: Int = 0,
    ) {
        fun toJuzStart() = JuzStart(juz = juz, surah = surah, ayah = ayah)
    }
}
