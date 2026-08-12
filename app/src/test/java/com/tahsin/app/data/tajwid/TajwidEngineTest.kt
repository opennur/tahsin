package com.tahsin.app.data.tajwid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes mesin aturan tajwid (rule-based, deterministik).
 *
 * Kata-kata memakai teks ber-tashkeel dengan urutan byte yang sama dengan
 * data equran.id (bundel aset): tanda menempel LANGSUNG setelah hurufnya
 * (mis. ن + ّ + َ, bukan urutan NFC َ lalu ّ) — engine membaca `word[i+1]`
 * sebagai tanda. Kata ber-shaddah ditulis sebagai Unicode escapes agar
 * urutannya tidak berubah jadi urutan kanonik NFC.
 *
 * [TajwidRule.letterIndex] menunjuk ke HURUF (bukan tanda).
 */
class TajwidEngineTest {

    // ---- Nun sukun / tanwin lintas kata ----

    @Test
    fun `nun sukun bertemu huruf ikhfa - Ikhfa Haqiqi`() {
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "قُلْ")
        assertEquals(listOf(RuleCategory.IKHFA), rules.map { it.category })
        assertEquals("Ikhfa' Haqiqi", rules.single().name)
        assertEquals(2, rules.single().letterIndex)
    }

    @Test
    fun `nun sukun bertemu ba - Iqlab`() {
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "بَعْدِ")
        assertEquals(listOf(RuleCategory.IQLAB), rules.map { it.category })
        assertEquals(2, rules.single().letterIndex)
    }

    @Test
    fun `nun sukun bertemu ya - Idgham Bighunnah`() {
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "يَقُولُ")
        assertEquals(listOf(RuleCategory.IDGHAM), rules.map { it.category })
        assertEquals("Idgham Bighunnah", rules.single().name)
    }

    @Test
    fun `nun sukun bertemu lam - Idgham Bilaghunnah`() {
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "لَدُنْ")
        assertEquals(listOf(RuleCategory.IDGHAM), rules.map { it.category })
        assertEquals("Idgham Bilaghunnah", rules.single().name)
    }

    @Test
    fun `nun sukun bertemu huruf halqi - Izhar Halqi`() {
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "عَلِمَ")
        assertEquals(listOf(RuleCategory.IZHAR), rules.map { it.category })
        assertEquals("Izhar Halqi", rules.single().name)
    }

    @Test
    fun `nun sukun bertemu hamza - Izhar Halqi`() {
        // Regresi: hamza (ء) sempat tidak dianggap huruf sehingga aturan
        // lintas kata yang bergantung padanya tidak pernah terdeteksi.
        val rules = TajwidEngine.analyzeWord("مِنْ", nextWord = "\u0621\u064E\u0627") // ءَا
        assertEquals(listOf(RuleCategory.IZHAR), rules.map { it.category })
        assertEquals("Izhar Halqi", rules.single().name)
    }

    @Test
    fun `nun sukun tanpa kata berikut - tidak ada hukum lintas kata`() {
        assertTrue(TajwidEngine.analyzeWord("مِنْ").isEmpty())
    }

    @Test
    fun `tanwin bertemu huruf halqi - Izhar Halqi`() {
        val rules = TajwidEngine.analyzeWord("عَلِيمٌ", nextWord = "حَكِيمٌ")
        // ي ber-kasra = mad thabi'i (huruf di index 4), lalu tanwin di م (index 5)
        // bertemu ح (halqi) = izhar — hukum di-anchor ke huruf م.
        assertEquals(
            listOf(RuleCategory.MAD, RuleCategory.IZHAR),
            rules.map { it.category },
        )
        assertEquals(4, rules[0].letterIndex)
        assertEquals(5, rules[1].letterIndex)
    }

    // ---- Tasydid ----

    @Test
    fun `nun bertasydid - Ghunnah Mushaddad`() {
        // إِنَّ  = إ + ن + ّ + َ
        val rules = TajwidEngine.analyzeWord("\u0625\u0646\u0651\u064E")
        assertEquals(listOf(RuleCategory.GHUNNAH), rules.map { it.category })
        assertEquals("Ghunnah (Mushaddad)", rules.single().name)
    }

    @Test
    fun `huruf non-nun bertasydid - Tasydid`() {
        // قَدَّرَ = ق َ د ّ َ ر َ — د bertasydid di index 2
        val rules = TajwidEngine.analyzeWord("\u0642\u064E\u062F\u0651\u064E\u0631\u064E")
        assertEquals(listOf(RuleCategory.SHADDAH), rules.map { it.category })
        assertEquals(2, rules.single().letterIndex)
    }

    // ---- Mad ----

    @Test
    fun `alif setelah fatha - Mad Thabi'i`() {
        val rules = TajwidEngine.analyzeWord("قَالَ")
        assertEquals(listOf(RuleCategory.MAD), rules.map { it.category })
        assertEquals("Mad Thabi'i", rules.single().name)
        assertEquals(2, rules.single().letterIndex)
    }

    @Test
    fun `mad bertemu hamza dalam satu kata - Mad Wajib Muttasil`() {
        val rules = TajwidEngine.analyzeWord("جَاءَ")
        assertEquals(listOf(RuleCategory.MAD), rules.map { it.category })
        assertEquals("Mad Wajib Muttasil", rules.single().name)
    }

    @Test
    fun `mad akhir kata bertemu hamza awal kata berikut - Mad Jaiz Munfasil`() {
        val rules = TajwidEngine.analyzeWord("فِي", nextWord = "أَنْفُسِهِمْ")
        assertEquals(listOf(RuleCategory.MAD), rules.map { it.category })
        assertEquals("Mad Jaiz Munfasil", rules.single().name)
    }

    @Test
    fun `alif khanjariah - Mad Thabi'i`() {
        val rules = TajwidEngine.analyzeWord("\u0645\u0670") // مٰ
        assertEquals(listOf(RuleCategory.MAD), rules.map { it.category })
        assertEquals("Mad Thabi'i", rules.single().name)
        assertEquals(0, rules.single().letterIndex)
    }

    // ---- Qalqalah ----

    @Test
    fun `huruf qalqalah bersukun - Qalqalah`() {
        val rules = TajwidEngine.analyzeWord("قَدْ")
        assertEquals(listOf(RuleCategory.QALQALAH), rules.map { it.category })
        assertEquals(2, rules.single().letterIndex)
    }

    // ---- Lam jalalah ----

    @Test
    fun `lam jalalah tanpa isyarat tebal - Tarqiq`() {
        // اللَّهُ = ا ل ل ّ َ ه ُ — لّ bertasydid di index 2
        val rules = TajwidEngine.analyzeWord("\u0627\u0644\u0644\u0651\u064E\u0647\u064F")
        assertTrue(rules.any { it.category == RuleCategory.LAM_JALALAH })
        assertEquals("Lam Jalalah Tarqiq", rules.first { it.category == RuleCategory.LAM_JALALAH }.name)
        assertEquals(2, rules.first { it.category == RuleCategory.LAM_JALALAH }.letterIndex)
    }

    @Test
    fun `lam jalalah setelah kata ber-fatha - Tafkhim`() {
        // prevWord لَا = ل َ ا — berakhiran fatha → lam dibaca tebal
        val rules = TajwidEngine.analyzeWord(
            "\u0627\u0644\u0644\u0651\u064E\u0647\u064F",
            prevWord = "\u0644\u064E\u0627",
        )
        assertTrue(rules.any { it.category == RuleCategory.LAM_JALALAH })
        assertEquals("Lam Jalalah Tafkhim", rules.first { it.category == RuleCategory.LAM_JALALAH }.name)
    }

    @Test
    fun `lam jalalah setelah kata ber-kasra - Tarqiq`() {
        // prevWord لِ = ل ِ — berakhiran kasra → lam dibaca tipis
        val rules = TajwidEngine.analyzeWord(
            "\u0627\u0644\u0644\u0651\u064E\u0647\u064F",
            prevWord = "\u0644\u0650",
        )
        assertTrue(rules.any { it.category == RuleCategory.LAM_JALALAH })
        assertEquals("Lam Jalalah Tarqiq", rules.first { it.category == RuleCategory.LAM_JALALAH }.name)
    }

    // ---- Multi-hukum + kata tanpa hukum ----

    @Test
    fun `satu kata bisa punya beberapa hukum - Ghunnah dan Mad`() {
        // إِنَّا = إ ن ّ َ ا — ghunnah di ن (1), mad thabi'i di ا (4)
        val rules = TajwidEngine.analyzeWord("\u0625\u0646\u0651\u064E\u0627")
        assertEquals(
            listOf(RuleCategory.GHUNNAH, RuleCategory.MAD),
            rules.map { it.category },
        )
        assertEquals(listOf(1, 4), rules.map { it.letterIndex })
    }

    @Test
    fun `kata tanpa hukum tajwid - kosong`() {
        assertTrue(TajwidEngine.analyzeWord("بِسْمِ").isEmpty())
    }
}
