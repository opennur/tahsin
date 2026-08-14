package org.opennur.tahsin.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.opennur.tahsin.data.ayatquiz.AyatQuiz
import org.opennur.tahsin.data.vocab.VocabKey
import org.opennur.tahsin.data.vocab.VocabularyEngine
import org.opennur.tahsin.data.vocab.VocabularyParser
import org.opennur.tahsin.util.AppLanguage
import java.io.File
import kotlin.random.Random

/**
 * INTEGRATION TEST (JVM) atas ASET BUNDEL ASLI — bukan fixture.
 * Membaca langsung file di `src/main/assets/quran/` dan memastikan:
 * 114 surah ter-parse, 6.236 ayat, jumlah ayat cocok `surah-list.json`,
 * teks Arab + terjemahan Indonesia non-kosong, `pages.json` 604 halaman,
 * `vocab.json` 1.200 entri terkurasi, dan round-trip parser → engine.
 */
class QuranAssetsIntegrationTest {

    private val assetsRoot: File by lazy {
        listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File(System.getProperty("user.dir"), "src/main/assets"),
            File(System.getProperty("user.dir"), "app/src/main/assets"),
        ).firstOrNull { it.isDirectory && File(it, "quran/pages.json").isFile }
            ?: error("aset bundel tidak ditemukan — jalankan tools/fetch_quran_data.py?")
    }

    private fun surahFile(n: Int) = File(assetsRoot, "quran/data/surah-$n.json")

    @Test
    fun `114 file surah asli terparse - semua punya ayat`() {
        for (n in 1..114) {
            val surah = QuranParser.parseSurah(surahFile(n).readText())
            assertTrue("surah $n ayat kosong", surah.ayahs.isNotEmpty())
            assertEquals("nomor surah $n salah", n, surah.number)
        }
    }

    @Test
    fun `total 6236 ayat dan jumlah ayat tiap surah cocok surah-list`() {
        val list = QuranParser.parseSurahList(File(assetsRoot, "quran/surah-list.json").readText())
        assertEquals(114, list.size)
        var total = 0
        for (meta in list) {
            val surah = QuranParser.parseSurah(surahFile(meta.number).readText())
            assertEquals(
                "jumlah ayat surah ${meta.number} tidak cocok surah-list",
                meta.ayahCount,
                surah.ayahs.size,
            )
            total += surah.ayahs.size
        }
        assertEquals(6236, total)
    }

    @Test
    fun `setiap ayat punya teks Arab dan terjemahan Indonesia non-kosong`() {
        var checked = 0
        for (n in 1..114) {
            val raw = surahFile(n).readText()
            val surah = QuranParser.parseSurah(raw)
            val translations = QuranParser.parseIdTranslations(raw)
            surah.ayahs.forEachIndexed { i, ayah ->
                assertTrue("surah $n ayat ${ayah.number} teks kosong", ayah.text.isNotBlank())
                val tr = translations.getOrNull(i).orEmpty()
                assertTrue("surah $n ayat ${ayah.number} terjemahan ID kosong", tr.isNotBlank())
                checked++
            }
        }
        assertEquals(6236, checked)
    }

    @Test
    fun `pages json asli - 604 halaman`() {
        val pages = MushafPagesParser.parse(File(assetsRoot, "quran/pages.json").readText())
        assertEquals(604, pages.pageCount)
        assertEquals(604, pages.pages.size)
    }

    @Test
    fun `vocab json asli - 1200 entri terkurasi lengkap ID dan EN`() {
        val entries = VocabularyParser.parse(File(assetsRoot, "quran/vocab.json").readText())
        assertEquals(1200, entries.size)
        entries.forEach { e ->
            assertTrue("arti ID kosong untuk ${e.key}", e.meaningId.isNotBlank())
            assertTrue("arti EN kosong untuk ${e.key}", e.meaningEn.isNotBlank())
        }
    }

    @Test
    fun `round-trip vocab - kata asli masuk meaningOfWord`() {
        val entries = VocabularyParser.parse(File(assetsRoot, "quran/vocab.json").readText())
        // 10 kata paling sering harus punya arti di kedua bahasa.
        val top = entries.sortedByDescending { it.freq }.take(10)
        top.forEach { e ->
            assertNotNull(
                "arti ID hilang untuk ${e.key}",
                VocabularyEngine.meaningOfWord(entries, e.word, AppLanguage.ID),
            )
            assertNotNull(
                "arti EN hilang untuk ${e.key}",
                VocabularyEngine.meaningOfWord(entries, e.word, AppLanguage.EN),
            )
        }
    }

    @Test
    fun `round-trip kuis ayat - soal valid dari ayat asli`() {
        val raw = surahFile(2).readText()
        val surah = QuranParser.parseSurah(raw)
        val pool = surah.ayahs.take(20).flatMap { it.words }.distinct()
        var generated = 0
        surah.ayahs.take(20).forEach { ayah ->
            val q = AyatQuiz.makeQuestion(2, ayah.number, ayah.words, pool, Random(1))
            if (q != null) {
                generated++
                assertEquals(4, q.options.size)
                assertTrue(q.correctWord in q.options)
            }
        }
        assertTrue("tidak ada soal yang bisa dibuat dari 20 ayat asli", generated > 0)
        assertEquals(
            VocabKey.normalize(pool.first()),
            VocabKey.normalize(pool.first()),
        )
    }
}
