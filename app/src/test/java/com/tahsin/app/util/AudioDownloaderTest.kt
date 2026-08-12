package com.tahsin.app.util

import com.tahsin.app.data.quran.Ayah
import com.tahsin.app.data.quran.Surah
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tes logika file & registry audio ([AudioDownloader]) — memakai direktori
 * temp (bukan Android Context) lewat konstruktor internal. Semua jalur yang
 * menyentuh jaringan (download HTTP) sengaja TIDAK dipicu: file dibuat dulu
 * atau dicatat sebagai missing, sehingga [ensureAyah]/[ensureWord]/
 * [downloadSurah] berhenti di cabang cache/registry.
 *
 * Catatan konvensi: kunci kata bersifat 1-based ([AudioUrls.wordKey] memakai
 * wordIndex + 1), jadi kata pertama sebuah ayat = `001_001_001.mp3`.
 */
class AudioDownloaderTest {

    private val slug = Reciter.MINSHAWY.slug
    private lateinit var audioDir: File
    private lateinit var reciterDir: File
    private lateinit var wbwDir: File

    @Before
    fun setUp() {
        audioDir = Files.createTempDirectory("audio-test").toFile()
        reciterDir = File(audioDir, slug)
        wbwDir = File(audioDir, "wbw")
    }

    @After
    fun tearDown() {
        audioDir.deleteRecursively()
    }

    private fun downloader(slugProvider: () -> String = { slug }) = AudioDownloader(audioDir, slugProvider)

    private fun write(dir: File, name: String, size: Int = 10): File {
        dir.mkdirs()
        val f = File(dir, name)
        f.writeBytes(ByteArray(size) { 1 })
        return f
    }

    private fun writeMissingAyahs(vararg keys: String) {
        File(audioDir, "missing-$slug.json").writeText("""{"ayahs":${gsonList(keys)},"words":[]}""")
    }

    private fun writeMissingWords(vararg keys: String) {
        File(audioDir, "missing-words.json").writeText("""{"ayahs":[],"words":${gsonList(keys)}}""")
    }

    private fun gsonList(items: Array<out String>) =
        items.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }

    // ---- path & registry missing ----

    @Test
    fun `ayahFile dan wordFile mengikuti konvensi folder dan nama`() {
        val d = downloader()
        assertEquals(File(reciterDir, "001002.mp3"), d.ayahFile(1, 2))
        assertEquals(File(wbwDir, "001_002_001.mp3"), d.wordFile(1, 2, 0)) // index kata 1-based
        assertEquals(File(wbwDir, "001_002_002.mp3"), d.wordFile(1, 2, 1))
    }

    @Test
    fun `registry missing ayat dibaca dari file dan dikenali`() {
        writeMissingAyahs("001002.mp3")
        val d = downloader()
        assertTrue(d.isAyahMissing(1, 2))
        assertFalse(d.isAyahMissing(1, 3))
        assertFalse(d.isAyahMissing(2, 2))
    }

    @Test
    fun `registry missing kata dibaca dari file dan dikenali`() {
        writeMissingWords("001_002_001.mp3")
        val d = downloader()
        assertTrue(d.isWordMissing(1, 2, 0))
        assertFalse(d.isWordMissing(1, 2, 1))
    }

    @Test
    fun `registry missing per qari - slug lain tidak terpengaruh`() {
        writeMissingAyahs("001002.mp3")
        val d = downloader { "Husary_128kbps" }
        assertFalse(d.isAyahMissing(1, 2))
        assertTrue(d.ayahFile(1, 2).parentFile!!.name == "Husary_128kbps")
    }

    @Test
    fun `migrasi format lama - missing json Minshawy ikut dibaca`() {
        File(audioDir, "missing.json").writeText("""{"ayahs":["001002.mp3"],"words":["001_001_001.mp3"]}""")
        val d = downloader()
        assertTrue("ayat legacy tidak dikenali", d.isAyahMissing(1, 2))
        assertTrue("kata legacy tidak dimigrasi", d.isWordMissing(1, 1, 0))
        // Kata legacy disimpan ke registry baru.
        assertTrue(File(audioDir, "missing-words.json").exists())
    }

    // ---- ensureAyah / ensureWord (cabang cache & registry, tanpa jaringan) ----

    @Test
    fun `ensureAyah - file cache ada langsung dikembalikan`() = runBlocking {
        val f = write(reciterDir, "001002.mp3")
        assertEquals(f, downloader().ensureAyah(1, 2))
    }

    @Test
    fun `ensureAyah - tercatat missing mengembalikan null tanpa mengunduh`() = runBlocking {
        writeMissingAyahs("001002.mp3")
        assertNull(downloader().ensureAyah(1, 2))
    }

    @Test
    fun `ensureWord - file cache ada langsung dikembalikan`() = runBlocking {
        val f = write(wbwDir, "001_002_001.mp3")
        assertEquals(f, downloader().ensureWord(1, 2, 0))
    }

    @Test
    fun `ensureWord - tercatat missing mengembalikan null`() = runBlocking {
        writeMissingWords("001_002_001.mp3")
        assertNull(downloader().ensureWord(1, 2, 0))
    }

    // ---- isSurahAudioComplete ----

    private fun surah(ayahs: List<Ayah>) = Surah(
        number = 1, nameArabic = "x", nameLatin = "x",
        ayahCount = ayahs.size, ayahs = ayahs,
    )

    private fun oneAyah() = Ayah(1, "بِسْمِ اللَّهِ") // 2 kata → 001_001_001 & 001_001_002

    @Test
    fun `isSurahAudioComplete - belum ada file apa pun - false`() {
        assertFalse(downloader().isSurahAudioComplete(surah(listOf(oneAyah()))))
    }

    @Test
    fun `isSurahAudioComplete - semua file ada - true`() {
        write(reciterDir, "001001.mp3")
        write(wbwDir, "001_001_001.mp3")
        write(wbwDir, "001_001_002.mp3")
        assertTrue(downloader().isSurahAudioComplete(surah(listOf(oneAyah()))))
    }

    @Test
    fun `isSurahAudioComplete - satu file hilang - false`() {
        write(reciterDir, "001001.mp3")
        write(wbwDir, "001_001_001.mp3")
        assertFalse(downloader().isSurahAudioComplete(surah(listOf(oneAyah()))))
    }

    @Test
    fun `isSurahAudioComplete - ayat missing di server tidak memblokir`() {
        writeMissingAyahs("001001.mp3")
        write(wbwDir, "001_001_001.mp3")
        write(wbwDir, "001_001_002.mp3")
        assertTrue(downloader().isSurahAudioComplete(surah(listOf(oneAyah()))))
    }

    @Test
    fun `isSurahAudioComplete - surah tanpa ayat - false`() {
        assertFalse(downloader().isSurahAudioComplete(surah(emptyList())))
    }

    // ---- downloadSurah (semua file sudah ada - tanpa jaringan) ----

    @Test
    fun `downloadSurah - semua file sudah ada - ok sama dengan total`() = runBlocking {
        write(reciterDir, "001001.mp3")
        write(wbwDir, "001_001_001.mp3")
        write(wbwDir, "001_001_002.mp3")

        val stats = downloader().downloadSurah(surah(listOf(oneAyah()))) { _, _ -> }
        assertEquals(3, stats.total)
        assertEquals(3, stats.ok)
    }

    @Test
    fun `downloadSurah - file missing di server dilewati dari total`() = runBlocking {
        writeMissingAyahs("001001.mp3")
        write(wbwDir, "001_001_001.mp3")
        write(wbwDir, "001_001_002.mp3")

        val stats = downloader().downloadSurah(surah(listOf(oneAyah()))) { _, _ -> }
        assertEquals(2, stats.total) // hanya kata yang dihitung
        assertEquals(2, stats.ok)
    }

    // ---- surahAudioInfo ----

    @Test
    fun `surahAudioInfo - menghitung file, ukuran, dan missing`() {
        write(reciterDir, "001001.mp3", size = 10)
        write(reciterDir, "001002.mp3", size = 20)
        write(wbwDir, "001_001_001.mp3", size = 5)
        write(wbwDir, "002_001_001.mp3", size = 99) // surah lain, tidak dihitung
        writeMissingAyahs("001003.mp3")
        writeMissingWords("001_001_002.mp3", "002_001_001.mp3")

        val info = downloader().surahAudioInfo(1, ayahCount = 2, totalWords = 3)
        assertEquals(2, info.ayahFiles)
        assertEquals(2, info.ayahCount)
        assertEquals(1, info.wordFiles)
        assertEquals(3, info.totalWords)
        assertEquals(10L + 20L + 5L, info.sizeBytes)
        assertEquals(1, info.missingAyahs)
        assertEquals(1, info.missingWords)
    }

    @Test
    fun `surahAudioInfo - tanpa file sama sekali`() {
        val info = downloader().surahAudioInfo(1, ayahCount = 2, totalWords = null)
        assertEquals(0, info.ayahFiles)
        assertEquals(0, info.wordFiles)
        assertNull(info.totalWords)
        assertEquals(0L, info.sizeBytes)
        assertEquals(0, info.missingAyahs)
        assertEquals(0, info.missingWords)
    }

    // ---- downloadedSurahNumbers ----

    @Test
    fun `downloadedSurahNumbers - dari file ayat dan kata, terurut unik`() {
        write(reciterDir, "001002.mp3")
        write(reciterDir, "114006.mp3")
        write(wbwDir, "002_001_001.mp3")
        write(wbwDir, "001_001_001.mp3")

        assertEquals(listOf(1, 2, 114), downloader().downloadedSurahNumbers())
    }

    @Test
    fun `downloadedSurahNumbers - tanpa file - kosong`() {
        assertTrue(downloader().downloadedSurahNumbers().isEmpty())
    }

    // ---- delete ----

    @Test
    fun `deleteSurahAudio - hanya menghapus file surah itu`() {
        write(reciterDir, "001001.mp3")
        write(reciterDir, "002001.mp3")
        write(wbwDir, "001_001_001.mp3")
        write(wbwDir, "002_001_001.mp3")

        downloader().deleteSurahAudio(1)

        assertFalse(File(reciterDir, "001001.mp3").exists())
        assertFalse(File(wbwDir, "001_001_001.mp3").exists())
        assertTrue(File(reciterDir, "002001.mp3").exists())
        assertTrue(File(wbwDir, "002_001_001.mp3").exists())
    }

    @Test
    fun `deleteAllAudio - menghapus seluruh folder audio`() {
        write(reciterDir, "001001.mp3")
        write(wbwDir, "001_001_001.mp3")

        downloader().deleteAllAudio()

        assertTrue(!audioDir.exists() || audioDir.listFiles()!!.isEmpty())
    }
}
