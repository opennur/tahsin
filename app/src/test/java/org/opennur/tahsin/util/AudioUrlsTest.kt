package org.opennur.tahsin.util

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

    @Test
    fun `konversi indeks kata vocab (1-based) ke audio (0-based)`() {
        // VocabExample.word 1-based (tools/build_vocab.py, enumerate start=1);
        // VocabularyViewModel.playCurrentWord memanggil playWord(word-1)
        // karena wordKey/wordUrl menambah 1 di dalamnya.
        assertEquals("002_004_008.mp3", AudioUrls.wordKey(2, 4, 8 - 1))
        assertEquals("001_001_002.mp3", AudioUrls.wordKey(1, 1, 2 - 1))
    }
}
