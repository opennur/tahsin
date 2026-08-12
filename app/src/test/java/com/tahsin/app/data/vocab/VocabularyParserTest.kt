package com.tahsin.app.data.vocab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes parsing & normalisasi kunci kosa kata (murni JVM). */
class VocabularyParserTest {

    private val sample = """
        {"entries":[
          {"key":"من","word":"مِنْ","translit":"min","meaningId":"dari","meaningEn":"from","freq":2763,
           "root":"من","rootMeaningId":"dari","rootMeaningEn":"from",
           "example":{"surah":2,"ayah":4,"word":8,"ayahArab":"بِسْمِ اللَّهِ","ayahLatin":"Bismillāh","ayahId":"Dengan nama Allah","ayahEn":"In the name of Allah"}},
          {"key":"قال","word":"قَالَ","translit":"qāla","meaningId":"dia berkata","meaningEn":"he said","freq":411,
           "root":"قول","rootMeaningId":"berkata; ucapan","rootMeaningEn":"to say; speech",
           "example":{"surah":2,"ayah":30,"word":2,"ayahArab":"قَالَ رَبُّكَ","ayahLatin":"Qāla rabbuka","ayahId":"Tuhanmu berfirman","ayahEn":"Your Lord said"}}
        ]}
    """.trimIndent()

    @Test
    fun `parse - semua field terbaca`() {
        val entries = VocabularyParser.parse(sample)
        assertEquals(2, entries.size)
        val first = entries[0]
        assertEquals("من", first.key)
        assertEquals("مِنْ", first.word)
        assertEquals("min", first.translit)
        assertEquals("dari", first.meaningId)
        assertEquals("from", first.meaningEn)
        assertEquals(2763, first.freq)
        assertEquals(2, first.example.surah)
        assertEquals(4, first.example.ayah)
        assertEquals(8, first.example.word)
        assertEquals("بِسْمِ اللَّهِ", first.example.ayahArab)
        assertEquals("Dengan nama Allah", first.example.ayahId)
        assertEquals("In the name of Allah", first.example.ayahEn)
    }

    @Test
    fun `parse - field akar kata terbaca`() {
        val entries = VocabularyParser.parse(sample)
        val qala = entries[1]
        assertEquals("قول", qala.root)
        assertEquals("berkata; ucapan", qala.rootMeaningId)
        assertEquals("to say; speech", qala.rootMeaningEn)
        // Kata tanpa field akar → default kosong (tidak crash).
        val missing = VocabularyParser.parse("""{"entries":[{"key":"من","meaningId":"dari"}]}""")
        assertEquals("", missing[0].root)
        assertEquals("", missing[0].rootMeaningId)
    }

    @Test
    fun `parse - JSON rusak atau entries null - daftar kosong (tidak crash)`() {
        assertTrue(VocabularyParser.parse("bukan json").isEmpty())
        assertTrue(VocabularyParser.parse("""{"entries":null}""").isEmpty())
        assertTrue(VocabularyParser.parse("").isEmpty())
    }

    @Test
    fun `parse - contoh kemunculan hilang memakai default aman`() {
        val json = """{"entries":[{"key":"من","word":"مِنْ","meaningId":"dari"}]}"""
        val entries = VocabularyParser.parse(json)
        assertEquals(1, entries.size)
        assertEquals(VocabExample(0, 0, 0, "", "", "", ""), entries[0].example)
        assertEquals(0, entries[0].freq)
    }

    @Test
    fun `VocabKey - strip harakat & tanda mushaf varian`() {
        assertEquals("من", VocabKey.normalize("مِنْ"))
        assertEquals("ما", VocabKey.normalize("مَا"))
        assertEquals("ما", VocabKey.normalize("مَآ"))          // maddah 0653
        assertEquals("به", VocabKey.normalize("بِهٖ"))          // subscript alef 0656
        assertEquals("يايها", VocabKey.normalize("يٰٓاَيُّهَا")) // dagger alif + maddah
    }

    @Test
    fun `VocabKey - seragamkan varian huruf`() {
        assertEquals("انسان", VocabKey.normalize("إِنْسَان"))   // hamza seat → alef
        assertEquals("شيا", VocabKey.normalize("شَيْـًٔا"))     // tatweel + hamza dibuang (alef seat tetap)
        assertEquals("الله", VocabKey.normalize("اللّٰهِ"))
        assertEquals("", VocabKey.normalize("ـ"))
    }
}
