package org.opennur.tahsin.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paginasi mushaf Madani: parser + lookup murni, plus TES EMAS atas
 * `assets/quran/pages.json` ASLI — jaminan paginasi "seperti mushaf sungguhan".
 */
class MushafPagesTest {

    private val assetsRoot: File by lazy {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File(System.getProperty("user.dir"), "src/main/assets"),
            File(System.getProperty("user.dir"), "app/src/main/assets"),
        )
        candidates.firstOrNull { it.isDirectory && File(it, "quran/pages.json").isFile }
            ?: error("assets/quran/pages.json tidak ditemukan — jalankan tes dari root project")
    }

    private val quranDir: File get() = File(assetsRoot, "quran")

    // ------------------------------------------------------------------
    // Parser
    // ------------------------------------------------------------------

    private val sampleJson = """
        {
          "schemaVersion": 1,
          "pageCount": 3,
          "pages": [
            {"page": 1, "segments": [{"surah": 1, "from": 1, "to": 7}]},
            {"page": 2, "segments": [{"surah": 2, "from": 1, "to": 5}]},
            {"page": 3, "segments": [{"surah": 2, "from": 6, "to": 8}, {"surah": 3, "from": 1, "to": 2}]}
          ],
          "juzStarts": [
            {"juz": 1, "surah": 1, "ayah": 1},
            {"juz": 2, "surah": 2, "ayah": 6}
          ]
        }
    """.trimIndent()

    @Test
    fun `parser - JSON valid dipetakan ke model`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertEquals(1, p.schemaVersion)
        assertEquals(3, p.pageCount)
        assertEquals(3, p.pages.size)
        assertEquals(7, p.pages.first().segments.first().toAyah)
        assertEquals(2, p.pages.last().segments.size)
        assertEquals(2, p.juzStarts.size)
        assertEquals(1 to 1, p.firstAyahOf(1))
        assertEquals(2 to 6, p.firstAyahOf(3))
    }

    @Test
    fun `parser - JSON rusak atau kosong menghasilkan paginasi kosong`() {
        val bad = MushafPagesParser.parse("bukan json{{{")
        assertEquals(0, bad.pageCount)
        assertEquals(0, bad.pages.size)
        assertEquals(0, bad.juzStarts.size)
        assertNull(bad.firstAyahOf(1))
        val empty = MushafPagesParser.parse("")
        assertEquals(0, empty.pageCount)
    }

    @Test
    fun `parser - JSON valid tapi field hilang memakai default kosong`() {
        // Objek utuh tapi tanpa kunci → semua cabang orEmpty() / elvis.
        val bare = MushafPagesParser.parse("""{"schemaVersion": 1}""")
        assertEquals(1, bare.schemaVersion)
        assertEquals(0, bare.pageCount)
        assertEquals(0, bare.pages.size)
        assertEquals(0, bare.juzStarts.size)
        // Halaman tanpa segmen → segmen kosong (firstAyahOf tetap null).
        val noSegments = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1}], "juzStarts": null}""",
        )
        assertEquals(1, noSegments.pages.size)
        assertEquals(0, noSegments.pages.first().segments.size)
        assertNull(noSegments.firstAyahOf(1))
        assertEquals(0, noSegments.juzStarts.size)
    }

    // ------------------------------------------------------------------
    // Lookup: pageOf / pagesOf / firstPageOf
    // ------------------------------------------------------------------

    @Test
    fun `pageOf - ayat di tengah halaman ditemukan`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertEquals(1, p.pageOf(1, 4))
        assertEquals(2, p.pageOf(2, 1))
        assertEquals(3, p.pageOf(2, 7))
        assertEquals(3, p.pageOf(3, 2))
    }

    @Test
    fun `pageOf - ayat di luar mushaf atau surah tak dikenal = null`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertNull(p.pageOf(1, 8))   // melewati ayat 7 Fatihah
        assertNull(p.pageOf(9, 1))   // surah tak ada di halaman mana pun
        assertNull(p.pageOf(3, 3))   // lewat dari surah terakhir
    }

    @Test
    fun `pagesOf dan firstPageOf - surah lintas halaman dan satu halaman`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertEquals(listOf(2, 3), p.pagesOf(2))
        assertEquals(2, p.firstPageOf(2))
        assertEquals(listOf(1), p.pagesOf(1))
        assertEquals(1, p.firstPageOf(1))
        assertNull(p.firstPageOf(4))
    }

    @Test
    fun `firstAyahOf - halaman tak dikenal null`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertNull(p.firstAyahOf(99))
    }

    // ------------------------------------------------------------------
    // Lookup: juz
    // ------------------------------------------------------------------

    @Test
    fun `juzOfPage - juz mengikuti ayat pertama halaman`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertEquals(1, p.juzOfPage(1))
        assertEquals(1, p.juzOfPage(2))     // 2:1 masih juz 1
        assertEquals(2, p.juzOfPage(3))     // 2:6 = awal juz 2
    }

    @Test
    fun `juzOfPage - halaman tak dikenal memakai juz pertama`() {
        val p = MushafPagesParser.parse(sampleJson)
        assertEquals(1, p.juzOfPage(99))
    }

    @Test
    fun `juzOfPage - juz mulai di tengah halaman tetap satu juz aktif`() {
        // Juz 2 mulai di 1:4 — halaman 1 (1:1-7) memuat batas juz di tengahnya.
        val mid = MushafPagesParser.parse(
            """{
              "pageCount": 2,
              "pages": [
                {"page": 1, "segments": [{"surah": 1, "from": 1, "to": 7}]},
                {"page": 2, "segments": [{"surah": 2, "from": 1, "to": 3}]}
              ],
              "juzStarts": [
                {"juz": 1, "surah": 1, "ayah": 1},
                {"juz": 2, "surah": 1, "ayah": 4},
                {"juz": 3, "surah": 3, "ayah": 1}
              ]
            }""".trimIndent(),
        )
        assertEquals(1, mid.juzOfPage(1))   // 1:1 → juz 1
        assertEquals(2, mid.juzOfPage(2))   // 2:1 → juz 2 (1:4 sudah lewat)
        // juzStartingOnPage hanya untuk juz yang mulai tepat di ayat pertama halaman.
        assertEquals(JuzStart(1, 1, 1), mid.juzStartingOnPage(1))
        assertNull(mid.juzStartingOnPage(2))
    }

    @Test
    fun `juzOfPage - juz di surah setelah halaman tidak aktif`() {
        val p = MushafPagesParser.parse(sampleJson)
        // Halaman 1 (1:1): juz 2 (2:6) punya surah lebih besar → tetap juz 1.
        assertEquals(1, p.juzOfPage(1))
        assertEquals(2, p.juzOfPage(3))
    }

    @Test
    fun `juzStartingOnPage - halaman awal juz dan halaman biasa`() {
        val p = MushafPagesParser.parse(sampleJson)
        val start = p.juzStartingOnPage(3)
        assertEquals(2, start?.juz)
        assertEquals(2, start?.surah)
        assertEquals(6, start?.ayah)
        // Halaman 1 = awal juz 1 (1:1).
        assertEquals(JuzStart(1, 1, 1), p.juzStartingOnPage(1))
        // Halaman 2 mulai 2:1 — bukan awal juz (juz 1 mulai di 1:1).
        assertNull(p.juzStartingOnPage(2))
        assertNull(p.juzStartingOnPage(99))
    }

    // ------------------------------------------------------------------
    // AyahNumbering
    // ------------------------------------------------------------------

    @Test
    fun `toArabicIndic - digit tunggal dan banyak`() {
        assertEquals("\u0661", AyahNumbering.toArabicIndic(1))
        assertEquals("\u0667", AyahNumbering.toArabicIndic(7))
        assertEquals("\u0660", AyahNumbering.toArabicIndic(10).substring(1))
        assertEquals("\u0662\u0668\u0666", AyahNumbering.toArabicIndic(286))
        assertEquals("\u0666\u0662\u0663\u0666", AyahNumbering.toArabicIndic(6236))
    }

    @Test
    fun `toArabicIndic - nomor tidak valid ditolak`() {
        try {
            AyahNumbering.toArabicIndic(0)
            assertFalse("harusnya throw", true)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("≥ 1"))
        }
    }

    @Test
    fun `endOfAyahMarker - memuat ۝ dan nomor Arab-Indik`() {
        assertEquals("\u06DD\u0661", AyahNumbering.endOfAyahMarker(1))
        assertEquals("\u06DD\u0662\u0668\u0666", AyahNumbering.endOfAyahMarker(286))
        assertTrue(AyahNumbering.endOfAyahMarker(7).startsWith("\u06DD"))
    }

    // ------------------------------------------------------------------
    // SajdahSigns
    // ------------------------------------------------------------------

    @Test
    fun `sajdah - 15 tempat sujud standar dan keempat yang wajib`() {
        assertEquals(15, SajdahSigns.ALL.size)
        val wajib = SajdahSigns.ALL.filter { it.obligatory }.map { it.surah to it.ayah }
        assertEquals(listOf(32 to 15, 41 to 38, 53 to 62, 96 to 19), wajib)
        // Beberapa titik patokan mushaf Madani.
        assertTrue(SajdahSigns.isSajdah(7, 206))
        assertTrue(SajdahSigns.isSajdah(16, 50))
        assertTrue(SajdahSigns.isSajdah(17, 109))
        assertTrue(SajdahSigns.isSajdah(27, 26))
        assertTrue(SajdahSigns.isSajdah(96, 19))
    }

    @Test
    fun `sajdah - ayat biasa bukan tempat sujud`() {
        assertFalse(SajdahSigns.isSajdah(1, 1))
        assertFalse(SajdahSigns.isSajdah(16, 49)) // pasangan: yang disujud adalah 16:50
        assertFalse(SajdahSigns.isSajdah(2, 255))
        assertEquals("\u06E9", SajdahSigns.SIGN)
    }

    // ------------------------------------------------------------------
    // Basmalah
    // ------------------------------------------------------------------

    @Test
    fun `basmalah - teks sama persis dengan ayat 1 Al-Fatihah bundel`() {
        val bundle = QuranParser.parseSurah(File(quranDir, "data/surah-1.json").readText()).ayahs.first().text
        assertEquals(bundle, Basmalah.TEXT)
        assertTrue(Basmalah.hasBasmalah(1))
        assertTrue(Basmalah.hasBasmalah(2))
        assertFalse(Basmalah.hasBasmalah(9))
        assertTrue(Basmalah.hasBasmalah(114))
    }

    @Test
    fun `basmalah - ornamen tidak untuk surah 1 dan 9`() {
        assertFalse(Basmalah.needsBasmalahOrnament(1))
        assertFalse(Basmalah.needsBasmalahOrnament(9))
        assertTrue(Basmalah.needsBasmalahOrnament(2))
        assertTrue(Basmalah.needsBasmalahOrnament(114))
    }

    // ------------------------------------------------------------------
    // TES EMAS: pages.json asli = mushaf Madani (604 halaman, 6236 ayat)
    // ------------------------------------------------------------------

    private val pagination: MushafPagination by lazy {
        MushafPagesParser.parse(File(quranDir, "pages.json").readText())
    }

    @Test
    fun `emas - 604 halaman, 30 juz, juz 1 di 1-1 dan juz 30 di 78-1`() {
        assertEquals(604, pagination.pageCount)
        assertEquals(604, pagination.pages.size)
        assertEquals(30, pagination.juzStarts.size)
        val j1 = pagination.juzStarts.first()
        val j30 = pagination.juzStarts.last()
        assertEquals(1, j1.juz); assertEquals(1, j1.surah); assertEquals(1, j1.ayah)
        assertEquals(30, j30.juz); assertEquals(78, j30.surah); assertEquals(1, j30.ayah)
        // Nomor juz di dalam daftar harus 1..30 berurutan.
        assertEquals((1..30).toList(), pagination.juzStarts.map { it.juz })
    }

    @Test
    fun `emas - seluruh 6236 ayat muncul tepat satu kali dan halaman monoton`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        assertEquals(114, surahs.size)
        val counts = IntArray(115) // indeks surah
        surahs.forEach { counts[it.number] = it.ayahCount }

        var lastKey = -1
        var total = 0
        for (page in pagination.pages) {
            assertTrue("segmen halaman ${page.page} kosong", page.segments.isNotEmpty())
            for (seg in page.segments) {
                assertTrue("segmen surah ${seg.surah} di luar 1..114", seg.surah in 1..114)
                assertTrue("rentang ayat terbalik di halaman ${page.page}",
                    seg.fromAyah <= seg.toAyah)
                assertTrue("segmen melebihi jumlah ayat surah ${seg.surah}",
                    seg.toAyah <= counts[seg.surah])
                for (ayah in seg.fromAyah..seg.toAyah) {
                    val key = seg.surah * 10_000 + ayah
                    assertTrue("urutan ayat tidak naik (halaman ${page.page})", key > lastKey)
                    lastKey = key
                    total++
                }
            }
        }
        assertEquals("total ayat harus 6236", 6236, total)
        // Setiap halaman berisi rentang bersambung — tidak ada surah yang
        // muncul dua kali terpisah di halaman yang sama.
        for (page in pagination.pages) {
            val surahsOnPage = page.segments.map { it.surah }
            assertEquals("halaman ${page.page}: surah ganda terpisah",
                surahsOnPage.distinct(), surahsOnPage)
        }
    }

    @Test
    fun `pageOf - halaman kosong di tengah pencarian menghasilkan null`() {
        // Halaman dengan segmen kosong (data rusak) — firstAyahOfPage null.
        val broken = MushafPagesParser.parse(
            """{
              "pageCount": 3,
              "pages": [
                {"page": 1, "segments": [{"surah": 1, "from": 1, "to": 2}]},
                {"page": 2, "segments": []},
                {"page": 3, "segments": []}
              ],
              "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]
            }""".trimIndent(),
        )
        assertNull(broken.pageOf(1, 3))  // melewati halaman kosong
        assertNull(broken.firstAyahOf(2))
    }

    @Test
    fun `emas - patokan halaman Madani yang terkenal`() {
        // Halaman 1 = Al-Fatihah utuh; halaman 2 mulai Al-Baqarah 1.
        assertEquals(1, pagination.pageOf(1, 1))
        assertEquals(1, pagination.pageOf(1, 7))
        assertEquals(2, pagination.pageOf(2, 1))
        // Halaman 604 = akhir mushaf (An-Nas).
        val last = pagination.pages.last()
        assertEquals(604, last.page)
        assertTrue(last.segments.any { it.surah == 114 && it.toAyah == 6 })
        // Ayat Kursi (2:255) dan penutup Al-Baqarah: halaman tidak boleh mundur.
        assertTrue(pagination.pageOf(2, 286)!! > pagination.pageOf(2, 1)!!)
        assertTrue(pagination.pageOf(2, 255)!! >= pagination.pageOf(2, 254)!!)
        // Total ayat dari semua segmen = 6236.
        val total = pagination.pages.sumOf { pg -> pg.segments.sumOf { it.ayahCount } }
        assertEquals(6236, total)
    }

    @Test
    fun `juzOfPage - tanpa daftar juz tetap 1 dan tidak crash`() {
        val noJuz = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 1, "from": 1, "to": 2}]}]}""",
        )
        assertEquals(1, noJuz.juzOfPage(1))   // loop juzStarts kosong → juz awal
        assertEquals(1, noJuz.juzOfPage(99))  // halaman tak dikenal → fallback 1
        assertNull(noJuz.juzStartingOnPage(1))
    }

    @Test
    fun `emas - juz aktif per halaman sesuai ayat pertama`() {
        val j2 = pagination.juzStarts.first { it.juz == 2 }
        val j2Page = pagination.pageOf(j2.surah, j2.ayah)!!
        assertEquals(2, pagination.juzOfPage(j2Page))
        assertEquals(1, pagination.juzOfPage(1))
        assertEquals(30, pagination.juzOfPage(604))
        // Halaman sebelum awal juz 2 masih juz 1.
        val beforeJ2 = pagination.pageOf(j2.surah, j2.ayah - 1)!!
        assertEquals(1, pagination.juzOfPage(beforeJ2))
        // Juz 1 dimulai di halaman 1; juz 30 dimulai di halaman yang berisi 78:1.
        assertEquals(JuzStart(1, 1, 1), pagination.juzStartingOnPage(1))
        val j30Page = pagination.pageOf(78, 1)!!
        assertEquals(JuzStart(30, 78, 1), pagination.juzStartingOnPage(j30Page))
    }

    @Test
    fun `emas - aturan basmalah terkunci (kecuali 9, Al-Fatihah via ayat 1)`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        // Ayat 1 setiap surah PASTI ada di halaman pertama surah itu (syarat
        // ornamen basmalah bisa tampil sebelum ayat 1).
        for (surah in surahs) {
            val firstPage = pagination.firstPageOf(surah.number) ?: error("surah ${surah.number} tanpa halaman")
            val page = pagination.pages.first { it.page == firstPage }
            assertTrue(
                "surah ${surah.number}: ayat 1 tidak ada di halaman pertama ($firstPage)",
                page.segments.any { it.surah == surah.number && it.fromAyah == 1 },
            )
        }
        // Ornamen basmalah: setiap surah KECUALI 9 (At-Taubah tanpa basmalah);
        // surah 1 (Al-Fatihah) memakai ayat 1-nya sebagai basmalah — keputusan user.
        assertFalse(Basmalah.needsBasmalahOrnament(1))
        assertFalse(Basmalah.needsBasmalahOrnament(9))
        for (n in listOf(2, 7, 13, 36, 114)) {
            assertTrue("surah $n harus berornamen basmalah", Basmalah.needsBasmalahOrnament(n))
        }
        // Aturan dasar: hasBasmalah = semua kecuali 9.
        assertFalse(Basmalah.hasBasmalah(9))
        assertTrue(Basmalah.hasBasmalah(1))
        assertTrue(Basmalah.hasBasmalah(114))
    }

    @Test
    fun `emas - komposisi halaman asli (basmalah sebelum ayat 1, tidak ganda di Al-Fatihah)`() {
        val surah1 = QuranParser.parseSurah(File(quranDir, "data/surah-1.json").readText())
        val surah2 = QuranParser.parseSurah(File(quranDir, "data/surah-2.json").readText())
        // Halaman 1 = Al-Fatihah: tidak ada ornamen (ayat 1-nya memang basmalah).
        val page1 = MushafPageComposer.composePage(pagination, 1, mapOf(1 to surah1))!!
        assertFalse(page1.ayahs.first().hasBasmalah)
        assertEquals(Basmalah.TEXT, page1.ayahs.first().text)
        // Halaman 2 = Al-Baqarah 1-5: ornamen basmalah HANYA sebelum ayat 1.
        val page2 = MushafPageComposer.composePage(pagination, 2, mapOf(2 to surah2))!!
        assertTrue(page2.ayahs.first().hasBasmalah)
        assertEquals(surah2.ayahs.first().text.trim(), page2.ayahs.first().text)
        assertTrue(page2.ayahs.drop(1).none { it.hasBasmalah })
    }

    @Test
    fun `emas - setiap ayat dapat ditemukan lewat pageOf dan firstPageOf konsisten`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        for (surah in surahs) {
            val pages = pagination.pagesOf(surah.number)
            assertTrue("surah ${surah.number} tanpa halaman", pages.isNotEmpty())
            assertEquals("halaman pertama surah ${surah.number}",
                pages.first(), pagination.firstPageOf(surah.number))
            // Ayat pertama & terakhir surah harus di halaman yang masuk akal.
            assertTrue(pagination.pageOf(surah.number, 1)!! <= pages.last())
            assertTrue(pagination.pageOf(surah.number, surah.ayahCount)!! >= pages.first())
        }
    }
}
