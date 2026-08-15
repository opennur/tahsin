package org.opennur.tahsin.util

import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.Surah
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

private data class FakeResponse(
    val status: Int,
    val body: ByteArray = byteArrayOf(),
    val headers: Map<String, String> = emptyMap(),
)

/** Minimal HTTP connection fake; unlike JDK HttpServer it is available in CI's test JDK. */
private class FakeHttpConnection(
    private val responseFor: (range: String?) -> FakeResponse,
) : HttpURLConnection(URL("https://audio.test/")) {

    private val requestHeaders = mutableMapOf<String, String>()
    private var cachedResponse: FakeResponse? = null

    private fun response(): FakeResponse = cachedResponse ?: responseFor(requestHeaders["Range"]).also {
        cachedResponse = it
    }

    override fun connect() {
        response()
    }

    override fun disconnect() = Unit

    override fun usingProxy(): Boolean = false

    override fun setRequestProperty(key: String, value: String) {
        requestHeaders[key] = value
    }

    override fun getResponseCode(): Int = response().status

    override fun getHeaderField(name: String): String? = response().headers[name]

    override fun getContentLengthLong(): Long = response().body.size.toLong()

    override fun getInputStream(): InputStream = ByteArrayInputStream(response().body)
}

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

    private fun networkDownloader(responseFor: (range: String?) -> FakeResponse) = AudioDownloader(
        audioDir,
        { slug },
        { FakeHttpConnection(responseFor) },
        retryBackoffMs = 1L,
    )

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

    @Test
    fun `ensureAyah - berhasil menulis part lalu rename atomik`() = runBlocking {
        val payload = "valid-mp3-payload".toByteArray()

        val file = networkDownloader { FakeResponse(200, payload) }.ensureAyah(1, 1)

        assertNotNull(file)
        assertArrayEquals(payload, file!!.readBytes())
        assertFalse(File(reciterDir, "001001.mp3.part").exists())
    }

    @Test
    fun `ensureAyah - melanjutkan part dengan HTTP Range`() = runBlocking {
        val payload = "resumable-mp3-payload".toByteArray()
        val file = networkDownloader { range ->
            val start = range?.substringAfter("bytes=")?.substringBefore("-")?.toIntOrNull()
            if (start == null) {
                FakeResponse(200, payload)
            } else {
                FakeResponse(
                    status = 206,
                    body = payload.copyOfRange(start, payload.size),
                    headers = mapOf("Content-Range" to "bytes $start-${payload.lastIndex}/${payload.size}"),
                )
            }
        }
        val part = File(reciterDir, "001001.mp3.part")
        part.parentFile!!.mkdirs()
        part.writeBytes(payload.copyOfRange(0, 7))

        val result = file.ensureAyah(1, 1)

        assertNotNull(result)
        assertArrayEquals(payload, result!!.readBytes())
        assertFalse(part.exists())
    }

    @Test
    fun `ensureAyah - transient HTTP error dicoba ulang`() = runBlocking {
        val attempts = AtomicInteger(0)
        val payload = "retry-payload".toByteArray()
        val file = networkDownloader {
            if (attempts.incrementAndGet() == 1) {
                FakeResponse(503)
            } else {
                FakeResponse(200, payload)
            }
        }.ensureAyah(1, 1)

        assertNotNull(file)
        assertEquals(2, attempts.get())
        assertArrayEquals(payload, file!!.readBytes())
    }

    @Test
    fun `downloadSurah - antrean tetap ada setelah kegagalan dan terbaca instance baru`() = runBlocking {
        val first = networkDownloader { FakeResponse(503) }
        val stats = first.downloadSurah(surah(listOf(oneAyah()))) { _, _ -> }

        assertEquals(0, stats.ok)
        assertTrue(first.pendingDownloads().any { it.surahNumber == 1 && it.reciterSlug == slug })
        assertTrue(networkDownloader { FakeResponse(503) }.pendingDownloads().any { it.surahNumber == 1 })
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
