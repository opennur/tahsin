package com.tahsin.app.util

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tes store status unduhan audio global ([DownloadProgress]). */
class DownloadProgressTest {

    @Before
    fun resetStore() {
        DownloadProgress.reset()
    }

    @Test
    fun `state awal - semua default`() {
        val s = DownloadProgress.state.value
        assertFalse(s.isDownloading)
        assertNull(s.currentSurahNumber)
        assertNull(s.currentSurahName)
        assertEquals(0, s.surahDone)
        assertEquals(0, s.surahTotal)
    }

    @Test
    fun `update mengubah state lewat transform`() {
        DownloadProgress.update {
            it.copy(
                isDownloading = true,
                currentSurahNumber = 2,
                currentSurahName = "Al-Baqarah",
            )
        }
        val s = DownloadProgress.state.value
        assertTrue(s.isDownloading)
        assertEquals(2, s.currentSurahNumber)
        assertEquals("Al-Baqarah", s.currentSurahName)
    }

    @Test
    fun `update bersifat kumulatif - transform kedua memakai nilai pertama`() {
        DownloadProgress.update { it.copy(surahDone = 3) }
        DownloadProgress.update { it.copy(surahTotal = it.surahDone + 7) }
        val s = DownloadProgress.state.value
        assertEquals(3, s.surahDone)
        assertEquals(10, s.surahTotal)
    }

    @Test
    fun `reset mengembalikan state awal`() {
        DownloadProgress.update { it.copy(isDownloading = true, surahTotal = 5) }
        DownloadProgress.reset()
        assertEquals(DownloadProgressState(), DownloadProgress.state.value)
    }

    @Test
    fun `stateFlow mengumumkan perubahan nilai`() = runBlocking {
        val seen = mutableListOf<DownloadProgressState>()
        val job = launch { DownloadProgress.state.collect { seen += it } }
        yield() // collector terdaftar & menerima nilai awal
        DownloadProgress.update { it.copy(isDownloading = true) }
        yield()
        job.cancel()
        assertTrue("harus ada lebih dari satu emisi", seen.size > 1)
        assertTrue(seen.any { it.isDownloading })
    }
}
