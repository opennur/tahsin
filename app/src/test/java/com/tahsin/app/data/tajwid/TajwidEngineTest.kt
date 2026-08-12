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
        // ق (isti'la) → tafkhim, د → tasydid, ر (fatha) → tafkhim ra'
        assertTrue(rules.any { it.category == RuleCategory.SHADDAH })
        val tasydid = rules.first { it.category == RuleCategory.SHADDAH }
        assertEquals("Tasydid", tasydid.name)
        assertEquals(2, tasydid.letterIndex)
        assertTrue(rules.any { it.category == RuleCategory.TAFKHIM })
    }

    // ---- Mad ----

    @Test
    fun `alif setelah fatha - Mad Thabi'i`() {
        // قَالَ — ق (isti'la) → tafkhim, ا → mad thabi'i
        val rules = TajwidEngine.analyzeWord("قَالَ")
        assertTrue(rules.any { it.category == RuleCategory.MAD })
        assertEquals("Mad Thabi'i", rules.first { it.category == RuleCategory.MAD }.name)
        assertEquals(2, rules.first { it.category == RuleCategory.MAD }.letterIndex)
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
        // Lanjut terus (wasl) → jaiz munfasil; TIDAK ada mad aridh (bukan akhir ayat).
        assertTrue(rules.any { it.name == "Mad Jaiz Munfasil" })
        assertTrue(rules.none { it.name == "Mad Aridh Lis-Sukun" })
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
        // قَدْ = ق َ د ْ — ق (isti'la) → tafkhim, د → qalqalah
        val rules = TajwidEngine.analyzeWord("قَدْ")
        assertTrue(rules.any { it.category == RuleCategory.QALQALAH })
        assertEquals(2, rules.first { it.category == RuleCategory.QALQALAH }.letterIndex)
        assertTrue(rules.any { it.category == RuleCategory.TAFKHIM })
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
        // إِنَّا = إ ن ّ َ ا — ghunnah di ن (1), mad thabi'i + mad aridh di ا (4)
        val rules = TajwidEngine.analyzeWord("\u0625\u0646\u0651\u064E\u0627")
        assertEquals(
            listOf(RuleCategory.GHUNNAH, RuleCategory.MAD, RuleCategory.MAD),
            rules.map { it.category },
        )
        assertEquals(listOf(1, 4, 4), rules.map { it.letterIndex })
    }

    @Test
    fun `kata tanpa hukum tajwid - kosong`() {
        assertTrue(TajwidEngine.analyzeWord("بِسْمِ").isEmpty())
    }

    // ---- Tafkhim / Tarqiq ----

    @Test
    fun `huruf isti'la selalu tafkhim`() {
        // خَالِدِينَ — خ (isti'la) → tafkhim, ا & ي → mad thabi'i
        val rules = TajwidEngine.analyzeWord("خَالِدِينَ")
        val tafkhim = rules.first { it.category == RuleCategory.TAFKHIM }
        assertEquals("Tafkhim (Huruf Isti'la)", tafkhim.name)
        assertEquals(0, tafkhim.letterIndex)
    }

    @Test
    fun `ra ber-fatha - tafkhim`() {
        // رَبِّ = ر َ ب ّ ِ
        val rules = TajwidEngine.analyzeWord("\u0631\u064E\u0628\u0651\u0650")
        val tafkhim = rules.first { it.category == RuleCategory.TAFKHIM }
        assertEquals("Tafkhim (Ra')", tafkhim.name)
    }

    @Test
    fun `ra ber-kasra - tarqiq`() {
        // رِزْقٍ — ر kasra → tarqiq di index 0
        val rules = TajwidEngine.analyzeWord("رِزْقٍ")
        assertTrue(rules.any { it.category == RuleCategory.TARQIQ })
        assertEquals(0, rules.first { it.category == RuleCategory.TARQIQ }.letterIndex)
    }

    @Test
    fun `ra sukun setelah kasra - tarqiq`() {
        // فِرْعَوْنَ = ف ِ ر ْ ع َ و ْ ن َ — ر sukun setelah kasra asli → tarqiq
        val rules = TajwidEngine.analyzeWord("\u0641\u0650\u0631\u0652\u0639\u064E\u0648\u0652\u0646\u064E")
        assertTrue(rules.any { it.category == RuleCategory.TARQIQ })
        assertEquals("Tarqiq (Ra')", rules.first { it.category == RuleCategory.TARQIQ }.name)
    }

    @Test
    fun `ra sukun diikuti huruf isti'la - tafkhim walau ada kasra sebelumnya`() {
        // مِرْصَاد = م ِ ر ْ ص َ ا د — ر sukun, ص isti'la ber-fatha setelahnya → TEBAL
        val rules = TajwidEngine.analyzeWord("\u0645\u0650\u0631\u0652\u0635\u064E\u0627\u062F")
        assertTrue(rules.any { it.category == RuleCategory.TAFKHIM })
        assertTrue(rules.none { it.category == RuleCategory.TARQIQ })
    }

    @Test
    fun `ra sukun setelah kasra di huruf isti'la - tafkhim`() {
        // قِرْطَاس = ق ِ ر ْ ط َ ا س — kasra ada di ق (isti'la) → bukan kasra asli → TEBAL
        val rules = TajwidEngine.analyzeWord("\u0642\u0650\u0631\u0652\u0637\u064E\u0627\u0633")
        assertTrue(rules.any { it.category == RuleCategory.TAFKHIM })
        assertTrue(rules.none { it.category == RuleCategory.TARQIQ })
    }

    // ---- Mad tambahan ----

    @Test
    fun `hamzah bertemu alif mad - Mad Badal`() {
        val rules = TajwidEngine.analyzeWord("آمَنَ")
        assertTrue(rules.any { it.name == "Mad Badal" })
        assertEquals(0, rules.first { it.name == "Mad Badal" }.letterIndex)
    }

    @Test
    fun `tanwin fatha di akhir kata - Mad Iwad`() {
        // رَحْمَةً — ر tafkhim + ة tanwin fatha → mad iwad
        val rules = TajwidEngine.analyzeWord("رَحْمَةً")
        assertTrue(rules.any { it.name == "Mad Iwad" })
    }

    @Test
    fun `huruf mad di akhir kata - Mad Aridh Lis-Sukun`() {
        val rules = TajwidEngine.analyzeWord("مَا")
        assertTrue(rules.any { it.name == "Mad Thabi'i" })
        assertTrue(rules.any { it.name == "Mad Aridh Lis-Sukun" })
    }

    // ---- Waqaf (berhenti / terus) ----

    @Test
    fun `tanda waqaf jaiz - boleh berhenti atau lanjut`() {
        val rules = TajwidEngine.analyzeWord("اَحَدٌ\u06DA") // ۚ
        assertTrue(rules.any { it.name == "Waqaf Jaiz" && it.category == RuleCategory.WAQAF })
    }

    @Test
    fun `tanda waqaf lazim - wajib berhenti`() {
        val rules = TajwidEngine.analyzeWord("\u0642\u064F\u0644\u0652\u06D8") // قُلْۘ
        assertTrue(rules.any { it.name == "Waqaf Lazim" })
    }

    @Test
    fun `tanda waqaf laa - jangan berhenti`() {
        val rules = TajwidEngine.analyzeWord("الرَّحِيمِ\u06D9") // ۙ
        assertTrue(rules.any { it.name == "Waqaf Laa" })
    }

    @Test
    fun `tanda waqaf wasl aula dan waqaf aula`() {
        assertTrue(TajwidEngine.analyzeWord("الْعٰلَمِيْنَ\u06D6").any { it.name == "Waqaf Wasl Aula" })
        assertTrue(TajwidEngine.analyzeWord("الدِّينِ\u06D7").any { it.name == "Waqaf Waqaf Aula" })
    }

    @Test
    fun `mad aridh hanya di akhir ayat - tidak di tengah`() {
        // Tanpa kata berikut (akhir ayat) → mad aridh muncul.
        assertTrue(TajwidEngine.analyzeWord("فِي").any { it.name == "Mad Aridh Lis-Sukun" })
        // Dengan kata berikut (tengah ayat) → tidak muncul.
        assertTrue(TajwidEngine.analyzeWord("فِي", nextWord = "قُلْ").none { it.name == "Mad Aridh Lis-Sukun" })
    }

    @Test
    fun `tanda waqaf di awal kata berikut - tetap terdeteksi`() {
        // ۚفَاذْهَبَا — tanda menempel di AWAL kata (bukan akhir)
        val rules = TajwidEngine.analyzeWord("\u06DA\u0641\u064E\u0627\u0630\u0652\u0647\u064E\u0628\u064E\u0627")
        assertTrue(rules.any { it.name == "Waqaf Jaiz" })
    }

    @Test
    fun `hamza berkursi di awal kata berikut - Mad Jaiz Munfasil`() {
        // ي (mad akhir) bertemu ئ (hamza berkursi) di awal kata berikut
        val rules = TajwidEngine.analyzeWord("فِي", nextWord = "\u0626\u064E\u0646")
        assertTrue(rules.any { it.name == "Mad Jaiz Munfasil" })
    }
}
