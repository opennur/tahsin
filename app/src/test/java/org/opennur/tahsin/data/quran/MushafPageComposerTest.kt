package org.opennur.tahsin.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Komposisi halaman mushaf (logika murni): paginasi + konten surah →
 * [ComposedPage] dengan ornamen otentik (basmalah, ۝ akhir ayat, ۩ sujud).
 */
class MushafPageComposerTest {

    /** Surah dengan nomor ayat eksplisit — daftar diindeks (ayat n − 1). */
    private fun surah(number: Int, nameAr: String, ayahTexts: Map<Int, String>) = Surah(
        number = number,
        nameArabic = nameAr,
        nameLatin = "Surah $number",
        ayahCount = ayahTexts.keys.maxOrNull() ?: 0,
        ayahs = (1..(ayahTexts.keys.maxOrNull() ?: 0)).map { n ->
            Ayah(n, ayahTexts[n] ?: "x")
        },
    )

    private val pagination = MushafPagesParser.parse(
        """
        {
          "schemaVersion": 1,
          "pageCount": 3,
          "pages": [
            {"page": 1, "segments": [{"surah": 1, "from": 1, "to": 3}]},
            {"page": 2, "segments": [{"surah": 2, "from": 1, "to": 2}]},
            {"page": 3, "segments": [{"surah": 2, "from": 3, "to": 3}, {"surah": 3, "from": 1, "to": 1}]}
          ],
          "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]
        }
        """.trimIndent(),
    )

    private val contents = mapOf(
        1 to surah(1, "الفاتحة", mapOf(1 to "أ", 2 to "ب", 3 to "ج")),
        2 to surah(2, "البقرة", mapOf(1 to "د", 2 to "ه", 3 to "و")),
        3 to surah(3, "آل عمران", mapOf(1 to "ز")),
    )

    @Test
    fun `komposisi - halaman sederhana dengan header dan penanda ayat`() {
        val page = MushafPageComposer.composePage(pagination, 1, contents)!!
        assertEquals(1, page.page)
        assertEquals(1, page.surahNumber)
        assertEquals("الفاتحة", page.surahNameArabic)
        assertEquals(1, page.juz)
        assertTrue(page.juzStartsOnPage)
        assertEquals(0, page.surahStartsMidPage)
        assertEquals(3, page.ayahs.size)
        val first = page.ayahs.first()
        assertEquals(1, first.number)
        assertFalse(first.isSajdah)
        assertFalse(first.hasBasmalah) // surah 1: ayat 1 sudah basmalah
        assertEquals("\u06DD\u0661", first.endMarker)
    }

    @Test
    fun `komposisi - basmalah ornamen di awal surah baru`() {
        // Halaman 3: surah 2 ayat 3 lalu surah 3 ayat 1 (baru mulai di tengah).
        val page = MushafPageComposer.composePage(pagination, 3, contents)!!
        assertEquals(2, page.surahNumber)
        assertEquals("البقرة", page.surahNameArabic)
        assertEquals(1, page.surahStartsMidPage)
        val surah3 = page.ayahs.last()
        assertEquals(3, surah3.surah)
        assertTrue(surah3.hasBasmalah)
        assertFalse(page.ayahs.first().hasBasmalah) // 2:3 bukan awal surah
    }

    @Test
    fun `komposisi - basmalah hanya di ayat 1, bukan tiap ayat segmen`() {
        // Segmen 2:1-5 (halaman awal Al-Baqarah) — ornamen HANYA sebelum ayat 1.
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 2, "from": 1, "to": 5}]}], "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]}""",
        )
        val surah2 = surah(2, "البقرة", mapOf(1 to "د", 2 to "ه", 3 to "و", 4 to "ز", 5 to "ح"))
        val page = MushafPageComposer.composePage(p, 1, mapOf(2 to surah2))!!
        assertEquals(5, page.ayahs.size)
        assertTrue(page.ayahs[0].hasBasmalah)   // sebelum ayat 1
        assertFalse(page.ayahs[1].hasBasmalah)  // ayat 2-5 TIDAK berornamen
        assertFalse(page.ayahs[2].hasBasmalah)
        assertFalse(page.ayahs[3].hasBasmalah)
        assertFalse(page.ayahs[4].hasBasmalah)
    }

    @Test
    fun `komposisi - surah 9 tanpa basmalah dan sujud ditandai`() {
        val surah9 = surah(9, "التوبة", mapOf(1 to "ت", 2 to "ث"))
        val p9 = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 9, "from": 1, "to": 2}]}], "juzStarts": [{"juz": 10, "surah": 9, "ayah": 1}]}""",
        )
        val page = MushafPageComposer.composePage(p9, 1, mapOf(9 to surah9))!!
        assertFalse(page.ayahs.first().hasBasmalah) // At-Tawbah: tidak ada basmalah
        assertEquals(10, page.juz)
    }

    @Test
    fun `komposisi - ayat sajdah membawa bendera dan teks bersih dari ۩`() {
        val sajdahSurah = surah(7, "الأعراف", mapOf(206 to "لَا يَسْجُدُوْنَ \u06E9"))
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 7, "from": 206, "to": 206}]}], "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]}""",
        )
        val page = MushafPageComposer.composePage(p, 1, mapOf(7 to sajdahSurah))!!
        val ayah = page.ayahs.first()
        assertTrue(ayah.isSajdah)
        assertFalse(ayah.sajdahObligatory) // 7:206 sunnah
        assertEquals("لَا يَسْجُدُوْنَ", ayah.text) // ۩ dibuang, spasi akhir dirapikan
    }

    @Test
    fun `komposisi - sajdah wajib dibedakan`() {
        val surah32 = surah(32, "السجدة", mapOf(15 to "يَسْجُدُوْنَ \u06E9"))
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 32, "from": 15, "to": 15}]}], "juzStarts": [{"juz": 21, "surah": 32, "ayah": 15}]}""",
        )
        val page = MushafPageComposer.composePage(p, 1, mapOf(32 to surah32))!!
        val ayah = page.ayahs.first()
        assertTrue(ayah.isSajdah)
        assertTrue(ayah.sajdahObligatory) // 32:15 fardhu
    }

    @Test
    fun `komposisi - halaman tak dikenal atau konten belum dimuat = null`() {
        assertNull(MushafPageComposer.composePage(pagination, 99, contents))
        // Halaman 2 butuh surah 2 — belum dimuat → null.
        assertNull(MushafPageComposer.composePage(pagination, 2, mapOf(1 to contents.getValue(1))))
        // Konten surah tidak lengkap (ayat kurang dari segmen) → null.
        val short = mapOf(2 to surah(2, "البقرة", mapOf(1 to "د")))
        assertNull(MushafPageComposer.composePage(pagination, 2, short))
    }

    @Test
    fun `komposisi - halaman dengan segmen kosong = data rusak, null`() {
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": []}], "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]}""",
        )
        assertNull(MushafPageComposer.composePage(p, 1, contents))
    }

    @Test
    fun `komposisi - ornamen akhir surah ࣖ dibuang dari teks tampilan`() {
        // equran.id menempelkan U+08D6 (ࣖ) di akhir ayat terakhir surah — banyak
        // font tidak punya glif itu dan merendernya sebagai kotak "[]".
        val surah1 = surah(1, "الفاتحة", mapOf(7 to "صِرَاطَ الَّذِيْنَ \u08D6"))
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 1, "from": 7, "to": 7}]}], "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]}""",
        )
        val page = MushafPageComposer.composePage(p, 1, mapOf(1 to surah1))!!
        assertEquals("صِرَاطَ الَّذِيْنَ", page.ayahs.first().text)
    }

    @Test
    fun `komposisi - segmen dengan rentang terbalik tidak menghasilkan ayat`() {
        // from > to (data rusak) → loop rentang kosong; halaman tetap tersusun.
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 2, "from": 3, "to": 2}]}], "juzStarts": [{"juz": 1, "surah": 1, "ayah": 1}]}""",
        )
        val page = MushafPageComposer.composePage(p, 1, contents)!!
        assertEquals(0, page.ayahs.size)
        assertEquals(2, page.surahNumber)
    }

    @Test
    fun `komposisi - sajdah 22-18 sunnah, 22-77 tidak membingungkan`() {
        // Dua sajdah dalam satu surah (22:18 dan 22:77) — cek `it.ayah ==` salah.
        val surah22 = surah(22, "الحج", mapOf(18 to "سُجِّدًا \u06E9"))
        val p = MushafPagesParser.parse(
            """{"pageCount": 1, "pages": [{"page": 1, "segments": [{"surah": 22, "from": 18, "to": 18}]}], "juzStarts": [{"juz": 17, "surah": 22, "ayah": 18}]}""",
        )
        val page = MushafPageComposer.composePage(p, 1, mapOf(22 to surah22))!!
        val ayah = page.ayahs.first()
        assertTrue(ayah.isSajdah)
        assertFalse(ayah.sajdahObligatory)
    }
}
