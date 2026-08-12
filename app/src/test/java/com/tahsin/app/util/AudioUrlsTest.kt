package com.tahsin.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes penamaan & deteksi file audio (kunci file, URL, nama file ayat). */
class AudioUrlsTest {

    @Test
    fun `kunci file ayat dan kata`() {
        assertEquals("001002.mp3", AudioUrls.ayahKey(1, 2))
        assertEquals("114006.mp3", AudioUrls.ayahKey(114, 6))
        assertEquals("001_002_003.mp3", AudioUrls.wordKey(1, 2, 2)) // wordIndex 0-based
    }

    @Test
    fun `deteksi nama file audio ayat - pola 6 digit plus mp3`() {
        assertTrue(AudioUrls.isAyahAudioFileName("001001.mp3"))
        assertTrue(AudioUrls.isAyahAudioFileName("114006.mp3"))
        assertFalse(AudioUrls.isAyahAudioFileName("001001"))        // tanpa ekstensi
        assertFalse(AudioUrls.isAyahAudioFileName("001.mp3"))       // digit kurang
        assertFalse(AudioUrls.isAyahAudioFileName("001001.m4a"))    // bukan mp3
        assertFalse(AudioUrls.isAyahAudioFileName("001_001_001.mp3")) // file kata
        assertFalse(AudioUrls.isAyahAudioFileName(""))              // kosong
    }

    @Test
    fun `URL kata tidak tergantung qari`() {
        assertEquals(
            "https://audio.qurancdn.com/wbw/001_002_003.mp3",
            AudioUrls.wordUrl(1, 2, 2),
        )
    }
}
