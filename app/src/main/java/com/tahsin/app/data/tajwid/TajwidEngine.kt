package com.tahsin.app.data.tajwid

import com.tahsin.app.util.ArabicNormalizer

/** Kategori hukum tajwid. */
enum class RuleCategory {
    MAD, GHUNNAH, IDGHAM, IKHFA, IQLAB, QALQALAH, LAM_JALALAH, IZHAR, SUKUN, SHADDAH,
}

/** Satu hukum tajwid yang terdeteksi pada sebuah kata. */
data class TajwidRule(
    val category: RuleCategory,
    val name: String,          // mis. "Idgham Bighunnah"
    val letterIndex: Int,      // indeks karakter (termasuk tanda) di dalam kata asli
    val explanation: String,   // penjelasan + cara baca yang benar
    /** Penjelasan bahasa Inggris (fallback: [explanation]). */
    val explanationEn: String = explanation,
)

/**
 * Mesin aturan tajwid berbasis teks (rule-based, deterministik).
 *
 * Bekerja pada kata ber-tashkeel hasil tokenisasi ayat. Mendeteksi hukum yang
 * bisa ditentukan dari teks saja: mad, ghunnah, idgham, ikhfa', iqlab,
 * qalqalah, lam jalalah, izhar. Konteks antar-kata (prevWord/nextWord)
 * dipakai untuk mad jaiz munfasil dan lam jalalah.
 *
 * CATATAN JUJUR: deteksi makhraj (bunyi) TIDAK bisa dilakukan dari teks —
 * itu butuh analisis audio; mesin ini memberi "peta hukum tajwid" kata,
 * dan STT membantu menemukan kata yang salah baca.
 */
object TajwidEngine {

    // Harakat
    private const val FATHA = '\u064E'
    private const val DAMMA = '\u064F'
    private const val KASRA = '\u0650'
    private const val SHADDAH = '\u0651'
    private const val SUKUN = '\u0652'
    private const val DAGGER_ALIF = '\u0670'
    private val TANWIN = setOf('\u064B', '\u064C', '\u064D')
    private val VOWELS = setOf(FATHA, DAMMA, KASRA, SHADDAH, SUKUN) + TANWIN
    /** Harakat yang "menempel di huruf" (sukun tidak termasuk — sukun = mad boleh). */
    private val VOWEL_ON_LETTER = setOf(FATHA, DAMMA, KASRA, SHADDAH) + TANWIN

    // Kelompok huruf
    private val IDGHAM_BIGHUNNAH = "ينمو".toSet()
    private val IDGHAM_BILA_GHUNNAH = "لر".toSet()
    private val IKHFA_LETTERS = "تثجدذزسشصضطظفقك".toSet()
    private val QALQALAH_LETTERS = "قطبجد".toSet()
    private val IZHAR_LETTERS = "ءهعحغخ".toSet()
    private val HAMZA_FORMS = "أإآء".toSet()

    /**
     * Analisis satu kata; `prevWord`/`nextWord` (teks Arab ber-tashkeel) memberi
     * konteks untuk lam jalalah & mad jaiz munfasil.
     */
    fun analyzeWord(word: String, prevWord: String? = null, nextWord: String? = null): List<TajwidRule> {
        val rules = mutableListOf<TajwidRule>()
        val n = word.length
        var i = 0
        while (i < n) {
            val c = word[i]
            if (!ArabicNormalizer.isLetter(c)) {
                i++
                continue
            }

            // tanda yang menempel pada huruf ini (bisa null / tanda mushaf)
            val mark = word.getOrNull(i + 1)?.takeIf { !ArabicNormalizer.isLetter(it) }

            // huruf berikutnya (lewati tanda)
            var j = i + 1
            while (j < n && !ArabicNormalizer.isLetter(word[j])) j++
            val nextLetter = if (j < n) word[j] else null

            // 1) Nun sukun / tanwin → idgham / ikhfa / iqlab / izhar
            //    Bisa lintas kata: nun sukun di akhir kata, huruf pertama kata berikut.
            val isNunSakin = c == 'ن' && mark == SUKUN
            val isTanwin = mark in TANWIN
            val effectiveNext = nextLetter
                ?: nextWord?.firstOrNull()?.takeIf { ArabicNormalizer.isLetter(it) }
            if ((isNunSakin || isTanwin) && effectiveNext != null) {
                when {
                    effectiveNext in IDGHAM_BIGHUNNAH -> rules += rule(
                        RuleCategory.IDGHAM, "Idgham Bighunnah", i,
                        "Nun sukun/tanwin bertemu ${effectiveNext}: dibaca idgham dengan ghunnah (dengung ±2 harakat).",
                        "Nun sakin/tanwin meets ${effectiveNext}: read merged with ghunnah (nasalization, ~2 counts).",
                    )
                    effectiveNext in IDGHAM_BILA_GHUNNAH -> rules += rule(
                        RuleCategory.IDGHAM, "Idgham Bilaghunnah", i,
                        "Nun sukun/tanwin bertemu ${effectiveNext}: dibaca idgham tanpa dengung, huruf ${effectiveNext} ditasydid.",
                        "Nun sakin/tanwin meets ${effectiveNext}: read merged without nasalization; the letter is doubled (tashdid).",
                    )
                    effectiveNext == 'ب' -> rules += rule(
                        RuleCategory.IQLAB, "Iqlab", i,
                        "Nun sukun/tanwin bertemu ب: berubah menjadi mim dengan ghunnah (dengung).",
                        "Nun sakin/tanwin meets ب: it changes into a meem with ghunnah (nasalization).",
                    )
                    effectiveNext in IKHFA_LETTERS -> rules += rule(
                        RuleCategory.IKHFA, "Ikhfa' Haqiqi", i,
                        "Nun sukun/tanwin bertemu ${effectiveNext}: dibaca samar antara izhar dan idgham dengan ghunnah 1–2 harakat.",
                        "Nun sakin/tanwin meets ${effectiveNext}: read vaguely between izhar and idgham with ghunnah 1–2 counts.",
                    )
                    effectiveNext in IZHAR_LETTERS -> rules += rule(
                        RuleCategory.IZHAR, "Izhar Halqi", i,
                        "Nun sukun/tanwin bertemu huruf halqi (${effectiveNext}): dibaca jelas tanpa dengung.",
                        "Nun sakin/tanwin meets a throat letter (${effectiveNext}): read clearly without nasalization.",
                    )
                }
            }

            // 2) Tasydid → ghunnah khusus نّ / مّ
            if (mark == SHADDAH) {
                if (c == 'ن' || c == 'م') {
                    rules += rule(
                        RuleCategory.GHUNNAH, "Ghunnah (Mushaddad)", i,
                        "Huruf ${c} bertasydid dibaca dengan dengung 2 harakat.",
                        "The mushaddad letter ${c} is read with nasalization for 2 counts.",
                    )
                } else {
                    rules += rule(
                        RuleCategory.SHADDAH, "Tasydid", i,
                        "Huruf ${c} bertasydid dibaca ganda (ditekan).",
                        "The mushaddad letter ${c} is read doubled (stressed).",
                    )
                }
            }

            // 3) Mad
            val prevVowel = findVowelBefore(word, i)
            val isMadLetter = when (c) {
                'ا' -> prevVowel == FATHA
                'ي' -> prevVowel == KASRA && !hasVowelAfter(word, i)
                'و' -> prevVowel == DAMMA && !hasVowelAfter(word, i)
                else -> false
            }
            if (mark == DAGGER_ALIF) {
                // alif khanjariah (مٰ / لٰ) = mad thabi'i
                rules += rule(RuleCategory.MAD, "Mad Thabi'i", i,
                    "Fatha diikuti alif khanjariah: dibaca panjang 2 harakat.",
                    "Fatha followed by a dagger alif: read long for 2 counts.")
            } else if (isMadLetter) {
                val hamzaAfter = nextLetter != null && nextLetter in HAMZA_FORMS
                if (hamzaAfter) {
                    rules += rule(RuleCategory.MAD, "Mad Wajib Muttasil", i,
                        "Mad bertemu hamza dalam satu kata: dibaca panjang 4–5 harakat (wajib).",
                        "Mad meets a hamzah in the same word: read long 4–5 counts (obligatory).")
                } else if (nextWord?.firstOrNull() in HAMZA_FORMS) {
                    rules += rule(RuleCategory.MAD, "Mad Jaiz Munfasil", i,
                        "Mad di akhir kata bertemu hamza di awal kata berikutnya: boleh dibaca 2 atau 4–5 harakat.",
                        "Mad at the end of a word meets a hamzah at the start of the next word: may be read 2 or 4–5 counts.")
                } else {
                    rules += rule(RuleCategory.MAD, "Mad Thabi'i", i,
                        "Mad thabi'i: dibaca panjang 2 harakat.",
                        "Natural lengthening (mad thabi'i): read long for 2 counts.")
                }
            }

            // 4) Qalqalah — huruf qalqalah bersukun
            if (c in QALQALAH_LETTERS && mark == SUKUN) {
                rules += rule(RuleCategory.QALQALAH, "Qalqalah", i,
                    "Huruf ${c} bersukun: dibaca memantul (guncangan suara).",
                    "The sukun letter ${c} is read with a bouncing echo (qalqalah).")
            }

            // 5) Lam jalalah — ل dalam الله
            if (c == 'ل' && mark == SHADDAH && isAllahWord(word)) {
                val prevMark = prevWord?.let { findVowelBefore(it, it.length - 1) }
                val thick = when {
                    prevMark == KASRA -> false
                    prevMark == FATHA || prevMark == DAMMA -> true
                    else -> mark == FATHA || mark == DAMMA // fallback: harakat pada لّ
                }
                rules += rule(
                    RuleCategory.LAM_JALALAH,
                    if (thick) "Lam Jalalah Tafkhim" else "Lam Jalalah Tarqiq",
                    i,
                    if (thick) "Lam pada الله dibaca tebal (tafkhim)."
                    else "Lam pada الله dibaca tipis (tarqiq).",
                    if (thick) "The lam in الله is read thick (tafkhim)."
                    else "The lam in الله is read thin (tarqiq).",
                )
            }

            i++
        }
        return rules.sortedBy { it.letterIndex }
    }

    // ---- helper ----

    private fun rule(category: RuleCategory, name: String, index: Int, explanation: String, explanationEn: String? = null) =
        TajwidRule(category, name, index, explanation, explanationEn ?: explanation)

    /** Harakat terakhir SEBELUM posisi `idx` (scan mundur; berhenti di huruf tanpa tanda). */
    private fun findVowelBefore(word: String, idx: Int): Char? {
        var k = idx - 1
        while (k >= 0) {
            val ch = word[k]
            if (ch in VOWELS) return ch
            if (ArabicNormalizer.isLetter(ch)) return null
            k--
        }
        return null
    }

    /** Apakah ada harakat (bukan sukun) tepat SETELAH posisi `idx`. */
    private fun hasVowelAfter(word: String, idx: Int): Boolean {
        val after = word.getOrNull(idx + 1) ?: return false
        return after in VOWEL_ON_LETTER
    }

    /** Kata ini bentuk "الله" (setelah normalisasi huruf dasar). */
    private fun isAllahWord(word: String): Boolean {
        val base = ArabicNormalizer.stripMarks(word).replace('ٱ', 'ا')
        return base == "الله" || base.endsWith("الله")
    }
}
