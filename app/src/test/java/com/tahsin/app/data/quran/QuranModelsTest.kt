package com.tahsin.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes model data mushaf: [Surah]/[Ayah] dan turunan `Ayah.words`
 * (tokenisasi kata untuk audio word-by-word & kuis tajwid).
 */
class QuranModelsTest {

    @Test
    fun `words memecah ayat menjadi kata ber-huruf Arab`() {
        val ayah = Ayah(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ")
        assertEquals(listOf("بِسْمِ", "اللَّهِ", "الرَّحْمَٰنِ"), ayah.words)
    }

    @Test
    fun `words membuang token yang hanya tanda, mempertahankan tanda yang menempel`() {
        // \u06DD = penanda akhir ayat (token sendiri → dibuang); \u06DA = tanda
        // waqaf jaiz yang menempel di awal kata → ikut dipertahankan (token
        // masih mengandung huruf). Perilaku ini di-pin agar tidak berubah diam-diam.
        val ayah = Ayah(2, "صِرَاطَ \u06DD \u06DAالَّذِينَ")
        assertEquals(listOf("صِرَاطَ", "\u06DAالَّذِينَ"), ayah.words)
    }

    @Test
    fun `words kosong untuk teks kosong atau hanya tanda`() {
        assertEquals(emptyList<String>(), Ayah(1, "").words)
        assertEquals(emptyList<String>(), Ayah(1, "   ").words)
        assertEquals(emptyList<String>(), Ayah(1, "\u06DD\u06DA").words)
    }

    @Test
    fun `translation default kosong`() {
        assertEquals("", Ayah(1, "بِسْمِ").translation)
    }

    @Test
    fun `default Surah - tanpa ayat dan jumlah ayat nol`() {
        val s = Surah(number = 1, nameArabic = "الفاتحة", nameLatin = "Al-Fatihah")
        assertEquals(0, s.ayahCount)
        assertTrue(s.ayahs.isEmpty())
    }

    @Test
    fun `data class - kesetaraan struktural`() {
        assertEquals(Ayah(1, "a", "tr"), Ayah(1, "a", "tr"))
        assertEquals(
            Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "بِسْمِ"))),
            Surah(1, "الفاتحة", "Al-Fatihah", 7, listOf(Ayah(1, "بِسْمِ"))),
        )
    }

    @Test
    fun `words tidak mengubah teks asli`() {
        val text = "الْحَمْدُ لِلَّهِ"
        val ayah = Ayah(1, text)
        ayah.words
        assertEquals(text, ayah.text) // properti turunan tidak memutasi data
    }
}
