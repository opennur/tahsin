package org.opennur.tahsin.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes pilihan font mushaf [ArabicFont] — konsistensi label/file/URL. */
class ArabicFontTest {

    @Test
    fun `urutan enum - UTSMANI lalu INDOPAK lalu ANDROID`() {
        assertEquals(
            listOf(ArabicFont.UTSMANI, ArabicFont.INDOPAK, ArabicFont.ANDROID),
            ArabicFont.entries,
        )
    }

    @Test
    fun `UTSMANI punya file dan sumber unduhan`() {
        assertEquals("uthmani.ttf", ArabicFont.UTSMANI.fileName)
        assertTrue(ArabicFont.UTSMANI.downloadUrl!!.startsWith("https://"))
        assertTrue(ArabicFont.UTSMANI.label.isNotBlank())
    }

    @Test
    fun `INDOPAK punya file tapi belum ada sumber unduhan`() {
        assertEquals("indopak.ttf", ArabicFont.INDOPAK.fileName)
        assertNull(ArabicFont.INDOPAK.downloadUrl)
    }

    @Test
    fun `ANDROID selalu font sistem - tanpa file dan URL`() {
        assertNull(ArabicFont.ANDROID.fileName)
        assertNull(ArabicFont.ANDROID.downloadUrl)
    }

    @Test
    fun `semua label terisi`() {
        ArabicFont.entries.forEach { assertTrue("label kosong: ${it.name}", it.label.isNotBlank()) }
    }
}
