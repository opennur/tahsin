package com.tahsin.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * TES EMAS INTEGRITAS MUSHAF — jaminan "satu harakat pun fatal".
 *
 * Memvalidasi DATA ASLI yang dibundel (`app/src/main/assets/quran/`), bukan
 * fiksin: jumlah surah/ayat harus persis mushaf standar (114 surah, 6236
 * ayat), jumlah ayat tiap surah harus persis daftar rujukan, dan TIDAK ADA
 * teks kosong (Arab, terjemahan ID, terjemahan EN) di ayat mana pun.
 *
 * Kalau tes ini gagal, artinya data mushaf rusak — jangan di-skip.
 */
class MushafIntegrityTest {

    private val assetsRoot: File by lazy {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File(System.getProperty("user.dir"), "src/main/assets"),
            File(System.getProperty("user.dir"), "app/src/main/assets"),
        )
        candidates.firstOrNull { it.isDirectory && File(it, "quran/surah-list.json").isFile }
            ?: error("Asset mushaf tidak ditemukan — jalankan tes dari root project")
    }

    private val quranDir: File get() = File(assetsRoot, "quran")

    /** Jumlah ayat standar mushaf Madinah (114 surah). Jumlah total = 6236. */
    private val STANDARD_AYAH_COUNTS = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99,
        128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38,
        29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18,
        12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29,
        19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8,
        11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
    )

    @Test
    fun `daftar surah - 114 surah dengan jumlah ayat standar (total 6236)`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        assertEquals(114, surahs.size)
        assertEquals(114, STANDARD_AYAH_COUNTS.size)

        surahs.forEachIndexed { i, surah ->
            assertEquals("jumlah ayat surah ${i + 1} (${surah.nameLatin}) salah",
                STANDARD_AYAH_COUNTS[i], surah.ayahCount)
            assertTrue("nama latin surah ${i + 1} kosong", surah.nameLatin.isNotBlank())
            assertTrue("nama arab surah ${i + 1} kosong", surah.nameArabic.isNotBlank())
        }
        assertEquals(6236, surahs.sumOf { it.ayahCount })
    }

    @Test
    fun `setiap surah - jumlah ayat, teks Arab, dan terjemahan ID tidak ada yang kosong`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        surahs.forEach { surah ->
            val raw = File(quranDir, "data/surah-${surah.number}.json").readText()
            val parsed = QuranParser.parseSurah(raw)
            val idTranslations = QuranParser.parseIdTranslations(raw)

            assertEquals("surah ${surah.number}: jumlah ayat tidak cocok",
                surah.ayahCount, parsed.ayahs.size)
            assertEquals("surah ${surah.number}: terjemahan ID tidak selengkap ayat",
                surah.ayahCount, idTranslations.size)

            parsed.ayahs.forEachIndexed { idx, ayah ->
                assertTrue("surah ${surah.number} ayat ${idx + 1}: teks Arab kosong",
                    ayah.text.isNotBlank())
                assertTrue("surah ${surah.number} ayat ${idx + 1}: terjemahan ID kosong",
                    idTranslations[idx].isNotBlank())
            }
        }
    }

    @Test
    fun `setiap surah - terjemahan EN tidak ada yang kosong`() {
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        surahs.forEach { surah ->
            val rawEn = File(quranDir, "data/trans-en-${surah.number}.json").readText()
            val en = QuranParser.parseEnTranslations(rawEn)
            assertEquals("surah ${surah.number}: terjemahan EN tidak selengkap ayat",
                surah.ayahCount, en.size)
            en.forEachIndexed { idx, text ->
                assertTrue("surah ${surah.number} ayat ${idx + 1}: terjemahan EN kosong",
                    text.isNotBlank())
            }
        }
    }

    @Test
    fun `setiap ayat - terpecah menjadi kata-kata non-kosong`() {
        // Konvensi pemecahan sama dengan engine (split(Regex("\\s+"))): spasi
        // ganda di data asli equran.id adalah artefak format, bukan kesalahan
        // teks — yang penting tidak ada kata kosong/hilang.
        val surahs = QuranParser.parseSurahList(File(quranDir, "surah-list.json").readText())
        surahs.forEach { surah ->
            val parsed = QuranParser.parseSurah(File(quranDir, "data/surah-${surah.number}.json").readText())
            parsed.ayahs.forEach { ayah ->
                val words = ayah.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                assertTrue("surah ${surah.number}: ayat kosong tidak boleh ada", words.isNotEmpty())
                // Kata-kata harus tetap utuh: pemecahan tidak boleh menghilangkan isi.
                assertEquals(
                    "surah ${surah.number}: pemecahan kata mengubah teks '${ayah.text.take(40)}'",
                    ayah.text.filterNot { it.isWhitespace() },
                    words.joinToString(""),
                )
            }
        }
    }
}
