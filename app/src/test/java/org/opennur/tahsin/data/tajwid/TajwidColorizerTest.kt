package org.opennur.tahsin.data.tajwid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tes pewarnaan mushaf: [TajwidColorizer.spans] mengubah daftar hukum menjadi
 * rentang karakter per kata. Logika yang diuji:
 * - Kategori yang tidak diwarnai (izhar, sukun, tasydid, tafkhim, tarqiq, waqaf)
 *   TIDAK menghasilkan span.
 * - Satu huruf hanya satu warna (rule terakhir menang).
 * - Span melebar melewati tanda harakat yang menempel pada huruf.
 * - Urutan span = urutan huruf.
 */
class TajwidColorizerTest {

    private fun rule(category: RuleCategory, index: Int) =
        TajwidRule(category, category.name, index, "penjelasan")

    @Test
    fun `tanpa aturan - tanpa span`() {
        assertTrue(TajwidColorizer.spans("قَالَ", emptyList()).isEmpty())
    }

    @Test
    fun `hanya kategori tak berwarna - tanpa span`() {
        val rules = listOf(
            rule(RuleCategory.IZHAR, 1),
            rule(RuleCategory.SUKUN, 2),
            rule(RuleCategory.SHADDAH, 3),
            rule(RuleCategory.TAFKHIM, 4),
            rule(RuleCategory.TARQIQ, 5),
            rule(RuleCategory.WAQAF, 6),
        )
        assertTrue(TajwidColorizer.spans("قَالَ", rules).isEmpty())
    }

    @Test
    fun `satu aturan mad - satu span dari huruf sampai sebelum huruf berikut`() {
        // قَالَ — mad thabi'i di ا (index 2); span berakhir di ل (huruf berikut).
        val rules = listOf(rule(RuleCategory.MAD, 2))
        val spans = TajwidColorizer.spans("قَالَ", rules)
        assertEquals(listOf(TajwidSpan(2, 3, RuleCategory.MAD)), spans)
    }

    @Test
    fun `span melebar melewati tanda harakat di akhir kata`() {
        // مِنْ — ikhfa di ن (index 2), sukun ْ (index 3) ikut terwarnai → span 2..4.
        val rules = listOf(rule(RuleCategory.IKHFA, 2))
        assertEquals(
            listOf(TajwidSpan(2, 4, RuleCategory.IKHFA)),
            TajwidColorizer.spans("مِنْ", rules),
        )
    }

    @Test
    fun `dua aturan di huruf berbeda - dua span urut naik`() {
        // قَالَ = ق(0) َ(1) ا(2) ل(3) َ(4) — qalqalah di ق (0) + mad di ا (2).
        // Span قalqalah melebar melewati fatha (1) → [0..2]; mad → [2..3].
        val rules = listOf(
            rule(RuleCategory.MAD, 2),
            rule(RuleCategory.QALQALAH, 0),
        )
        assertEquals(
            listOf(
                TajwidSpan(0, 2, RuleCategory.QALQALAH),
                TajwidSpan(2, 3, RuleCategory.MAD),
            ),
            TajwidColorizer.spans("قَالَ", rules),
        )
    }

    @Test
    fun `dua aturan pada huruf yang sama - aturan terakhir menang`() {
        // إِنَّا = \u0625(0) \u0650(1) ن(2) ّ(3) َ(4) ا(5) — dua aturan di anchor ن
        // (index 2): span melebar melewati tasydid & fatha sampai huruf berikut (ا).
        // Catatan: ditulis sebagai \u escapes agar urutan tanda tidak diubah
        // urutan kanonik NFC oleh editor (lihat catatan TajwidEngineTest).
        val word = "\u0625\u0650\u0646\u0651\u064E\u0627"
        val rules = listOf(
            rule(RuleCategory.GHUNNAH, 2),
            rule(RuleCategory.QALQALAH, 2),
        )
        assertEquals(
            listOf(TajwidSpan(2, 5, RuleCategory.QALQALAH)),
            TajwidColorizer.spans(word, rules),
        )
    }

    @Test
    fun `aturan tak berwarna dicampur aturan berwarna - hanya yang berwarna`() {
        // قَدْ — tafkhim (ق, tak berwarna) + qalqalah (د, berwarna).
        val rules = listOf(
            rule(RuleCategory.TAFKHIM, 0),
            rule(RuleCategory.QALQALAH, 2),
        )
        assertEquals(
            listOf(TajwidSpan(2, 4, RuleCategory.QALQALAH)), // ْ ikut terwarnai
            TajwidColorizer.spans("قَدْ", rules),
        )
    }

    @Test
    fun `span konsisten dengan hasil engine untuk kata asli`() {
        // Integrasi ringan: engine → colorizer untuk kalimat basmalah.
        val word = "\u0627\u0644\u0644\u0651\u064E\u0647\u0650" // اللَّهِ
        val rules = TajwidEngine.analyzeWord(word)
        val spans = TajwidColorizer.spans(word, rules)
        // Semua span valid: dalam jangkauan, urut, dan tidak tumpang tindih.
        var prevEnd = 0
        for (span in spans) {
            assertTrue("start=${span.start}", span.start in 0 until word.length)
            assertTrue("end=${span.end}", span.end in (span.start + 1)..word.length)
            assertTrue("tumpang tindih", span.start >= prevEnd)
            prevEnd = span.end
        }
    }


    @Test
    fun `span - getter konsisten + kategori diwarnai + enum lengkap`() {
        // Tanpa rule → kosong.
        assertTrue(org.opennur.tahsin.data.tajwid.TajwidColorizer.spans("قل", emptyList()).isEmpty())

        val rules = listOf(
            org.opennur.tahsin.data.tajwid.TajwidRule(
                org.opennur.tahsin.data.tajwid.RuleCategory.MAD, "Mad", 1,
                "panjang", "panjang",
            ),
        )
        val spans = org.opennur.tahsin.data.tajwid.TajwidColorizer.spans("قَالَ", rules)
        assertEquals(1, spans.size)
        val span = spans[0]
        assertEquals(1, span.start)
        assertEquals(2, span.end) // berhenti di huruf berikutnya ("ل")
        assertEquals(org.opennur.tahsin.data.tajwid.RuleCategory.MAD, span.category)
        assertEquals(13, org.opennur.tahsin.data.tajwid.RuleCategory.entries.size)
    }
}
