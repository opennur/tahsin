package org.opennur.tahsin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes pilihan qari' (URL everyayah.com) dan kecepatan audio. */
class ReciterTest {

    // ---- URL per qari' ----

    @Test
    fun `URL ayat default tetap Minshawy (kompatibel)`() {
        assertEquals(
            "https://everyayah.com/data/Minshawy_Murattal_128kbps/001002.mp3",
            AudioUrls.ayahUrl(1, 2),
        )
    }

    @Test
    fun `URL ayat mengikuti qari' yang dipilih`() {
        assertEquals(
            "https://everyayah.com/data/Husary_128kbps/001002.mp3",
            AudioUrls.ayahUrl(1, 2, Reciter.HUSARY),
        )
        assertEquals(
            "https://everyayah.com/data/Husary_Muallim_128kbps/114006.mp3",
            AudioUrls.ayahUrl(114, 6, Reciter.HUSARY_MUALLIM),
        )
    }

    @Test
    fun `semua slug qari' tercatat di everyayah (tidak ada yang kosong)`() {
        Reciter.entries.forEach { r ->
            assertTrue("slug kosong untuk ${r.name}", r.slug.isNotBlank())
            assertTrue("label kosong untuk ${r.name}", r.label.isNotBlank())
        }
    }

    // ---- fromSlug ----

    @Test
    fun `fromSlug mengenali slug yang dikenal`() {
        assertEquals(Reciter.ALAFASY, Reciter.fromSlug("Alafasy_128kbps"))
        assertEquals(Reciter.HUSARY, Reciter.fromSlug("Husary_128kbps"))
    }

    @Test
    fun `fromSlug fallback ke Minshawy untuk slug tak dikenal atau null`() {
        assertEquals(Reciter.MINSHAWY, Reciter.fromSlug("Qari_Tidak_Ada_999kbps"))
        assertEquals(Reciter.MINSHAWY, Reciter.fromSlug(null))
        assertEquals(Reciter.MINSHAWY, Reciter.fromSlug(""))
    }

    // ---- kecepatan audio ----

    @Test
    fun `opsi kecepatan terurut dan dalam rentang`() {
        assertEquals(listOf(0.5f, 0.75f, 1.0f, 1.25f), AudioSpeeds.options)
    }

    @Test
    fun `clamp membatasi ke rentang 0,5 sampai 1,25`() {
        assertEquals(0.5f, AudioSpeeds.clamp(0.1f), 0.0001f)
        assertEquals(1.25f, AudioSpeeds.clamp(3.0f), 0.0001f)
        assertEquals(0.75f, AudioSpeeds.clamp(0.75f), 0.0001f)
    }

    @Test
    fun `format kecepatan ringkas`() {
        assertEquals("0.5×", AudioSpeeds.format(0.5f))
        assertEquals("0.75×", AudioSpeeds.format(0.75f))
        assertEquals("1×", AudioSpeeds.format(1.0f))
        assertEquals("1.25×", AudioSpeeds.format(1.25f))
    }
}
