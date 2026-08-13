package org.opennur.tahsin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes pencarian ayat: normalisasi Arab (harakat, hamza, ya, ta marbuta) dan
 * kata kunci terjemahan ID/EN (case-insensitive).
 */
class AyahSearchTest {

    private fun ayah(
        arabic: String = "",
        id: String = "",
        en: String = "",
        surah: Int = 1,
        no: Int = 1,
    ) = SearchableAyah(surah, no, arabic, id, en)

    // ---- Arab (normalisasi) ----

    @Test
    fun `kata Arab dengan harakat - query tanpa harakat cocok`() {
        val a = ayah(arabic = "الرَّحْمَٰنِ الرَّحِيمِ")
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "الرحمن"))
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "رحمن"))
    }

    @Test
    fun `varian hamza dan alif diseragamkan`() {
        assertTrue(AyahSearch.matches("الْقُرْآنِ", "", "", "القران"))
        assertTrue(AyahSearch.matches("إِنَّ", "", "", "ان"))
        assertTrue(AyahSearch.matches("آيَاتِ", "", "", "ايات"))
    }

    @Test
    fun `ta marbuta dan ya diseragamkan`() {
        assertTrue(AyahSearch.matches("الرَّحْمَةِ", "", "", "الرحمة"))
        assertTrue(AyahSearch.matches("هُدًى", "", "", "هدي"))
    }

    @Test
    fun `hamza berkursi dilipat (momen tanpa hamza tetap ketemu)`() {
        assertTrue(AyahSearch.matches("مُؤْمِنَ", "", "", "مومن"))
        assertTrue(AyahSearch.matches("بِئْسَ", "", "", "بيس"))
        assertTrue(AyahSearch.matches("يُؤْمِنُونَ", "", "", "يومنون"))
    }

    @Test
    fun `awalan kata Arab cocok (query sebagian kata)`() {
        val a = ayah(arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "بسم"))
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "الله"))
    }

    // ---- Terjemahan ----

    @Test
    fun `kata kunci terjemahan Indonesia cocok`() {
        assertTrue(AyahSearch.matches("", "Maka nikmat Tuhanmu yang manakah yang kamu dustakan?", "", "nikmat"))
        assertTrue(AyahSearch.matches("", "Sesungguhnya orang-orang kafir itu", "", "kafir"))
    }

    @Test
    fun `kata kunci terjemahan Inggris cocok - case insensitive`() {
        assertTrue(AyahSearch.matches("", "", "Oft-Forgiving, Most Merciful", "forgiving"))
        assertTrue(AyahSearch.matches("", "", "Oft-Forgiving, Most Merciful", "FORGIVING"))
        assertTrue(AyahSearch.matches("", "", "In the name of Allah", "all"))
        assertFalse(AyahSearch.matches("", "", "Oft-Forgiving, Most Merciful", "gracious"))
    }

    @Test
    fun `query kosong tidak cocok apa pun`() {
        val a = ayah(arabic = "الرَّحْمَٰنِ", id = "Yang Maha Pengasih", en = "The Most Gracious")
        assertFalse(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, ""))
        assertFalse(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "   "))
    }

    // ---- search() ----

    @Test
    fun `search memfilter dan mengurutkan hasil (surah, ayat)`() {
        val index = listOf(
            ayah(arabic = "الرَّحْمَٰنِ", surah = 2, no = 2),
            ayah(id = "rahmat", surah = 3, no = 1),
            ayah(id = "pengasih", surah = 1, no = 3),
            ayah(en = "most gracious", surah = 1, no = 1),
        )
        // Query "rahmat" hanya cocok di 3:1 — "gracious" tidak mengandung "rahmat".
        val results = AyahSearch.search(index, "rahmat")
        assertEquals(listOf(3 to 1), results.map { it.surahNumber to it.ayahNumber })

        // Query "pengasih" cocok di 1:3; urutan hasil (surah, ayat) terurut.
        val results2 = AyahSearch.search(index, "pengasih")
        assertEquals(listOf(1 to 3), results2.map { it.surahNumber to it.ayahNumber })
    }

    @Test
    fun `search mengurutkan hasil lintas surah`() {
        val index = listOf(
            ayah(id = "rahmat", surah = 3, no = 1),
            ayah(id = "rahmat", surah = 1, no = 2),
            ayah(id = "rahmat", surah = 2, no = 5),
        )
        val results = AyahSearch.search(index, "rahmat")
        assertEquals(
            listOf(1 to 2, 2 to 5, 3 to 1),
            results.map { it.surahNumber to it.ayahNumber },
        )
    }

    @Test
    fun `search membatasi jumlah hasil`() {
        val index = (1..100).map { n -> ayah(id = "rahmat", surah = 1, no = n) }
        val results = AyahSearch.search(index, "rahmat", limit = 10)
        assertEquals(10, results.size)
    }

    @Test
    fun `search query kosong mengembalikan daftar kosong`() {
        val index = listOf(ayah(id = "rahmat"))
        assertTrue(AyahSearch.search(index, "").isEmpty())
    }

    @Test
    fun `search limit nol atau negatif - daftar kosong (tidak crash)`() {
        val index = listOf(ayah(id = "rahmat"))
        assertTrue(AyahSearch.search(index, "rahmat", limit = 0).isEmpty())
        assertTrue(AyahSearch.search(index, "rahmat", limit = -1).isEmpty())
        assertTrue(AyahSearch.search(index, "rahmat", limit = -100).isEmpty())
    }

    @Test
    fun `search membuang spasi di sekitar query`() {
        val index = listOf(ayah(id = "rahmat", surah = 1, no = 1))
        assertEquals(1, AyahSearch.search(index, "  rahmat  ").size)
        assertEquals(1, AyahSearch.search(index, "\trahmat\n").size)
    }

    @Test
    fun `query Arab ber-harakat dinormalisasi sebelum dicocokkan`() {
        val a = ayah(arabic = "الرَّحْمَٰنِ")
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "الرَّحْمَٰنِ"))
        assertTrue(AyahSearch.matches(a.arabic, a.translationId, a.translationEn, "الرحمن"))
    }

    @Test
    fun `search - ayat yang cocok di Arab dan terjemahan hanya muncul sekali`() {
        val index = listOf(ayah(arabic = "رَحْمَة", id = "rahmat", surah = 1, no = 1))
        val results = AyahSearch.search(index, "rahmat")
        assertEquals(1, results.size)
        assertEquals(1 to 1, results.single().surahNumber to results.single().ayahNumber)
    }

    @Test
    fun `search - hasil diurutkan surah lalu ayat`() {
        val index = listOf(
            ayah(id = "rahmat", surah = 2, no = 1),
            ayah(id = "rahmat", surah = 1, no = 5),
            ayah(id = "rahmat", surah = 1, no = 1),
            ayah(id = "rahmat", surah = 2, no = 2),
        )
        val results = AyahSearch.search(index, "rahmat")
        assertEquals(4, results.size)
        assertEquals(
            listOf(1 to 1, 1 to 5, 2 to 1, 2 to 2),
            results.map { it.surahNumber to it.ayahNumber },
        )
    }
}
