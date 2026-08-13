package org.opennur.tahsin.data.tajwid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gap-coverage cabang-cabang jarang [TajwidEngine.analyzeWord] — tiap kondisi
 * diuji kedua sisinya, demi jaminan "tidak ada harakat yang salah" di mesin
 * tajwid. Kata memakai urutan byte Uthmani sama dengan data equran.id
 * (tanda menempel LANGSUNG setelah huruf: ن + ّ + َ), ditulis sebagai
 * Unicode escapes supaya tidak berubah menjadi urutan NFC.
 */
class TajwidEngineBranchTest {

    private fun names(word: String, prev: String? = null, next: String? = null): List<String> =
        TajwidEngine.analyzeWord(word, prev, next).map { it.name }

    private fun has(word: String, rule: String, prev: String? = null, next: String? = null) =
        assertTrue("${names(word, prev, next)} harus memuat $rule", rule in names(word, prev, next))

    private fun not(word: String, rule: String, prev: String? = null, next: String? = null) =
        assertFalse("${names(word, prev, next)} TIDAK boleh memuat $rule", rule in names(word, prev, next))

    // ---- nun sukun / tanwin: lintas kata ----

    @Test
    fun `nun sukun di akhir kata - huruf pertama kata berikut menentukan`() {
        has("مِنْ", "Idgham Bighunnah", next = "يَشَاءُ")
        has("مِنْ", "Idgham Bilaghunnah", next = "لَدُنْكِ")
        has("مِنْ", "Ikhfa' Haqiqi", next = "سُوءٍ")
        has("مِنْ", "Izhar Halqi", next = "عَمَلٍ")
        has("مِنْ", "Iqlab", next = "بَعْدِ")
    }

    @Test
    fun `nun sukun tanpa kata berikut - tidak ada hukum`() {
        assertTrue(names("مِنْ").isEmpty())
    }

    @Test
    fun `tanwin lintas kata - idgham bila ghunnah`() {
        has("رَحِيمٌ", "Idgham Bilaghunnah", next = "رَبِّ")
    }

    // ---- tasydid (urutan byte: ن + ّ + َ) ----

    @Test
    fun `nun bertasydid - ghunnah mushaddad`() {
        has("\u0625\u0646\u0651\u064E", "Ghunnah (Mushaddad)") // إِنَّ
        has("\u062B\u0645\u0651\u064E", "Ghunnah (Mushaddad)") // ثُمَّ
    }

    @Test
    fun `huruf lain bertasydid - tasydid`() {
        // قَدَّرَ — د bertasydid; ق isti'la → tafkhim; ر fatha → tafkhim
        val rules = TajwidEngine.analyzeWord("\u0642\u064E\u062F\u0651\u064E\u0631\u064E")
        assertTrue(rules.any { it.category == RuleCategory.SHADDAH })
        assertTrue(rules.any { it.name == "Tasydid" })
        assertEquals(2, rules.first { it.category == RuleCategory.SHADDAH }.letterIndex)
    }

    // ---- mad ----

    @Test
    fun `alif khanjariah - mad thabi'i`() {
        has("\u0645\u0670", "Mad Thabi'i") // مٰ
    }

    @Test
    fun `mad wajib muttasil`() {
        has("سُوءَ", "Mad Wajib Muttasil") // و setelah damma bertemu hamza
    }

    @Test
    fun `mad jaiz munfasil lintas kata`() {
        has("بِمَا", "Mad Jaiz Munfasil", next = "أَنْزَلَ")
        not("بِمَا", "Mad Jaiz Munfasil", next = "قَدَرَ")
    }

    @Test
    fun `mad aridh lis-sukun di akhir ayat`() {
        val namesAridh = names("لَا")
        assertTrue("Mad Thabi'i" in namesAridh)
        assertTrue("Mad Aridh Lis-Sukun" in namesAridh)
        // Bukan akhir ayat (ada kata berikut) → tidak ada mad aridh.
        not("لَا", "Mad Aridh Lis-Sukun", next = "إِلَهَ")
    }

    @Test
    fun `mad iwad di akhir ayat - tanwin fathah pada ta marbutah`() {
        has("\u0631\u064E\u062D\u0645\u064E\u0629\u064B", "Mad Iwad") // رَحْمَةً
        not("\u0631\u064E\u062D\u0645\u064E\u0629\u064B", "Mad Iwad", next = "بَعْدَ")
    }

    @Test
    fun `huruf mad yang tidak memenuhi syarat - bukan mad thabi'i`() {
        not("بَيْت", "Mad Thabi'i")     // ي setelah fatha
        not("قُوَّة", "Mad Thabi'i")    // و setelah damma tapi ada harakat setelahnya
        not("اِسْم", "Mad Thabi'i")     // ا di awal tanpa fatha sebelumnya
    }

    // ---- qalqalah ----

    @Test
    fun `qalqalah - hanya huruf bersukun`() {
        has("\u0642\u064E\u062F\u0652", "Qalqalah") // قَدْ — د sukun
        not("قَالَ", "Qalqalah") // ق/ل berharakat → bukan qalqalah
    }

    // ---- lam jalalah ----

    @Test
    fun `lam jalalah - tarqiq setelah kasrah`() {
        has("\u0627\u0644\u0644\u0651\u064E\u0647\u064F", "Lam Jalalah Tarqiq",
            prev = "\u0641\u0650\u064A") // فِي — berakhiran kasra
    }

    @Test
    fun `lam jalalah - tafkhim setelah fatha atau damma`() {
        has("\u0627\u0644\u0644\u0651\u064E\u0647\u064F", "Lam Jalalah Tafkhim",
            prev = "\u0644\u064E\u0627") // لَا — berakhiran fatha
        has("\u0627\u0644\u0644\u0651\u064E\u0647\u064F", "Lam Jalalah Tafkhim",
            prev = "\u0642\u064F\u0648") // قُو — berakhiran damma
    }

    @Test
    fun `lam jalalah - fallback tanpa harakat prev (tarqiq)`() {
        has("\u0627\u0644\u0644\u0651\u064E\u0647\u064F", "Lam Jalalah Tarqiq")
    }

    @Test
    fun `lam jalalah - kata berakhiran allah (isAllahWord endsWith)`() {
        // عَبْدُاللَّهِ — لّ bertasydid, prev null → fallback tarqiq.
        has("\u0639\u064E\u0628\u0652\u062F\u064F\u0627\u0644\u0644\u0651\u064E\u0647\u0650", "Lam Jalalah Tarqiq")
    }

    @Test
    fun `lam bertasydid di luar kata allah - tidak ada lam jalalah`() {
        not("بَلَّ", "Lam Jalalah Tafkhim")
        not("بَلَّ", "Lam Jalalah Tarqiq")
    }

    // ---- ra' ----

    @Test
    fun `ra - kasrah dibaca tipis`() {
        has("رِزْقًا", "Tarqiq (Ra')")
    }

    @Test
    fun `ra - fatha atau damma dibaca tebal`() {
        has("رَحْمَة", "Tafkhim (Ra')")
        has("رُزْقًا", "Tafkhim (Ra')")
    }

    @Test
    fun `ra sukun - tebal karena diikuti isti'la berfatha`() {
        has("مِرْصَاد", "Tafkhim (Ra')")
    }

    @Test
    fun `ra sukun - tipis setelah kasrah asli`() {
        has("فِرْعَوْنَ", "Tarqiq (Ra')")
    }

    @Test
    fun `ra sukun - tebal default (tanpa kasrah sebelum)`() {
        has("مَرْيَم", "Tafkhim (Ra')")
    }

    @Test
    fun `ra sukun di awal kata - letterBefore null tetap aman`() {
        has("\u0631\u0652\u0628", "Tafkhim (Ra')") // رْب
    }

    // ---- isti'la ----

    @Test
    fun `isti'la selalu tafkhim`() {
        has("صَلَاة", "Tafkhim (Huruf Isti'la)")
    }

    // ---- waqaf ----

    @Test
    fun `tanda waqaf - terus (ۚ) dan berhenti (ۘ)`() {
        assertTrue(names("\u06DA\u0641\u064E\u0627\u0630\u0652\u0647\u064E\u0628\u064E\u0627")
            .any { it.startsWith("Waqaf") }) // ۚفَاذْهَبَا
        assertTrue(names("\u0642\u064F\u0644\u0652\u06D8").any { it.startsWith("Waqaf") }) // قُلْۘ
    }

    // ---- konsistensi ----

    @Test
    fun `aturan - penjelasan EN tidak pernah kosong`() {
        for (rule in TajwidEngine.analyzeWord("\u0627\u0644\u0631\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650")) {
            assertFalse(rule.explanation.isBlank())
            assertFalse(rule.explanationEn.isBlank())
        }
    }


    // ---- vektor ekstrem cabang ----

    @Test
    fun `mad - ya setelah kasra tapi ada harakat setelahnya - bukan mad`() {
        not("فِيَّ", "Mad Thabi'i") // فِيَّ
    }

    @Test
    fun `nun berharakat selain sukun - tidak ada hukum nun`() {
        assertTrue(names("نَعْبُدُ").isEmpty()) // نَعْبُدُ
    }

    @Test
    fun `nun sukun dengan nextWord kosong - aman`() {
        assertTrue(names("مِنْ", next = "").isEmpty())
    }

    @Test
    fun `ra sukun - diikuti isti'la berdamah - tebal`() {
        has("رْصُو", "Tafkhim (Ra')") // رْصُو
    }

    @Test
    fun `ra sukun - diikuti isti'la berkasra - tebal (kasra bukan penebal)`() {
        has("رْصِ", "Tafkhim (Ra')") // رْصِ
    }

    @Test
    fun `ra sukun di akhir kata - tebal default`() {
        has("مَرْ", "Tafkhim (Ra')") // مَرْ
    }

    @Test
    fun `mad ber-sukun di akhir ayat - bukan aridh (harus bersambung harakat)`() {
        not("فِيْ", "Mad Aridh Lis-Sukun") // فِيْ
    }


    // ---- vektor cabang null (Char? == Char) ----

    @Test
    fun `nun tanpa tanda setelahnya - tidak crash dan tidak ada hukum nun`() {
        assertTrue(names("مِن").isEmpty())   // مِن — ن di akhir, mark null
        assertTrue(names("ن").isEmpty())               // ن tunggal
    }

    @Test
    fun `ya tanpa vokal sebelumnya - bukan mad`() {
        not("\u064A\u064E\u0648\u0652\u0645", "Mad Thabi'i") // يَوْم — ي di awal, prevVowel null
    }

    @Test
    fun `lam jalalah - prevWord berakhir sukun - tarqiq (damma tidak cocok)`() {
        has("اللَّهُ", "Lam Jalalah Tarqiq", prev = "مِنْ")
    }

    @Test
    fun `ra sukun - diikuti isti'la tanpa harakat - tebal`() {
        has("رْص", "Tafkhim (Ra')") // رْص — ص di akhir, vowelOnLetter null
    }


    @Test
    fun `mad akhir ayat dengan tanda waqaf - aridh tetap berlaku`() {
        // قُلُوۚ — و setelah damma di akhir + tanda waqaf ۚ (mark non-sukun).
        val rules = TajwidEngine.analyzeWord("قُلُوۚ")
        assertTrue(rules.any { it.name == "Mad Aridh Lis-Sukun" })
    }


    @Test
    fun `lam jalalah - prevWord bertanwin - tarqiq`() {
        has("اللَّهُ", "Lam Jalalah Tarqiq",
            prev = "أَحَدً") // أَحَدٌ — prevMark tanwin
    }


    @Test
    fun `lam jalalah - prevMark tanwin dan tasydid - tarqiq`() {
        has("اللَّهُ", "Lam Jalalah Tarqiq",
            prev = "دًا")        // دًا — prevMark fatha-tanwin
        has("اللَّهُ", "Lam Jalalah Tarqiq",
            prev = "بَلَّ") // بَلَّ — prevMark shaddah
    }
}
