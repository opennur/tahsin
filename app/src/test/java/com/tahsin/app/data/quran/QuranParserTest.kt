package com.tahsin.app.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Tes parsing JSON mushaf (murni JVM) — format equran.id + quran.com. */
class QuranParserTest {

    @Test
    fun `parseSurahList - metadata 114 surah`() {
        val json = """
            {"surahs":[
                {"number":1,"nameArabic":"الفاتحة","nameLatin":"Al-Fatihah","ayahCount":7},
                {"number":2,"nameArabic":"البقرة","nameLatin":"Al-Baqarah","ayahCount":286}
            ]}
        """.trimIndent()
        val list = QuranParser.parseSurahList(json)
        assertEquals(2, list.size)
        assertEquals(Surah(1, "الفاتحة", "Al-Fatihah", 7), list[0])
        assertEquals(Surah(2, "البقرة", "Al-Baqarah", 286), list[1])
        assertTrue(list.all { it.ayahs.isEmpty() })
    }

    @Test
    fun `parseSurahList - daftar kosong`() {
        assertTrue(QuranParser.parseSurahList("""{"surahs":[]}""").isEmpty())
    }

    @Test
    fun `parseSurah - teks Arab per ayat, tanpa terjemahan`() {
        val json = """
            {"data":{"nomor":1,"nama":"الفاتحة","namaLatin":"Al-Fatihah","jumlahAyat":7,
              "ayat":[
                {"nomorAyat":1,"teksArab":"بِسْمِ اللَّهِ","teksIndonesia":"Dengan nama Allah"},
                {"nomorAyat":2,"teksArab":"الْحَمْدُ","teksIndonesia":"Segala puji"}
              ]}}
        """.trimIndent()
        val surah = QuranParser.parseSurah(json)
        assertEquals(1, surah.number)
        assertEquals("الفاتحة", surah.nameArabic)
        assertEquals("Al-Fatihah", surah.nameLatin)
        assertEquals(7, surah.ayahCount)
        assertEquals(2, surah.ayahs.size)
        assertEquals(Ayah(1, "بِسْمِ اللَّهِ"), surah.ayahs[0])   // translation kosong di parseSurah
        assertEquals(Ayah(2, "الْحَمْدُ"), surah.ayahs[1])
    }

    @Test
    fun `parseSurah - respons tanpa ayat`() {
        val surah = QuranParser.parseSurah(
            """{"data":{"nomor":112,"nama":"الإخلاص","namaLatin":"Al-Ikhlas","jumlahAyat":4,"ayat":[]}}""",
        )
        assertEquals(112, surah.number)
        assertEquals(4, surah.ayahCount)
        assertTrue(surah.ayahs.isEmpty())
    }

    @Test
    fun `parseSurah - data null melempar IOException`() {
        assertThrows(IOException::class.java) { QuranParser.parseSurah("""{"data":null}""") }
    }

    @Test
    fun `parseIdTranslations - terjemahan Indonesia per ayat`() {
        val json = """
            {"data":{"ayat":[
                {"nomorAyat":1,"teksArab":"a","teksIndonesia":"Dengan nama Allah"},
                {"nomorAyat":2,"teksArab":"b","teksIndonesia":"Segala puji"}
            ]}}
        """.trimIndent()
        assertEquals(
            listOf("Dengan nama Allah", "Segala puji"),
            QuranParser.parseIdTranslations(json),
        )
    }

    @Test
    fun `parseIdTranslations - JSON tidak valid = daftar kosong`() {
        assertTrue(QuranParser.parseIdTranslations("bukan json").isEmpty())
    }

    @Test
    fun `parseEnTranslations - tag HTML dan footnote sup dibersihkan`() {
        val json = """
            {"translations":[
                {"resource_id":20,"text":"In the name of Allah<sup footnote=\"1\">, the Entirely Merciful, the Especially Merciful.</sup>"},
                {"resource_id":20,"text":"All praise is due to <b>Allah</b>, Lord of the worlds"}
            ]}
        """.trimIndent()
        assertEquals(
            listOf(
                "In the name of Allah",
                "All praise is due to Allah, Lord of the worlds",
            ),
            QuranParser.parseEnTranslations(json),
        )
    }

    @Test
    fun `stripHtml - berbagai tag dan spasi berlebih`() {
        assertEquals("a b c", QuranParser.stripHtml("a <b>b</b> c"))
        assertEquals("text more", QuranParser.stripHtml("text <sup>1</sup>more"))
        assertEquals("x y", QuranParser.stripHtml("x   <i> y </i>"))
    }

    @Test
    fun `buildEnCacheJson - hasil sudah bersih dan round-trip konsisten`() {
        val json = """
            {"translations":[
                {"resource_id":20,"text":"a<sup footnote=\"1\">b</sup> c"},
                {"resource_id":20,"text":"d <i>e</i> f"}
            ]}
        """.trimIndent()
        val cache = QuranParser.buildEnCacheJson(json)
        assertTrue(cache != null && !cache.contains("<sup") && !cache.contains("<i>"))
        // Round-trip: parse dari versi cache menghasilkan teks bersih yang sama.
        assertEquals(
            QuranParser.parseEnTranslations(json),
            QuranParser.parseEnTranslations(cache!!),
        )
    }

    @Test
    fun `buildEnCacheJson - JSON tidak valid = null (tidak ditulis ke cache)`() {
        assertNull(QuranParser.buildEnCacheJson("bukan json"))
    }
}
