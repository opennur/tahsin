package org.opennur.tahsin.data.tajwid

import kotlin.random.Random

/**
 * Satu soal kuis tajwid: satu kata (dengan konteks kata sekitar), satu hukum
 * sasaran, dan 4 opsi pilihan ganda.
 */
data class QuizQuestion(
    val word: String,
    val prevWord: String?,
    val nextWord: String?,
    val targetRule: TajwidRule,
    val options: List<String>,
)

/**
 * Generator soal kuis tajwid ("hukum apa pada kata ini?") — murni, bisa diuji.
 * Memanfaatkan hasil [TajwidEngine.analyzeWord] yang sudah ada.
 */
object TajwidQuiz {

    const val OPTION_COUNT = 4

    /** Kolam nama aturan untuk pengecoh (semua yang bisa dihasilkan engine). */
    private val DISTRACTOR_POOL = listOf(
        "Idgham Bighunnah", "Idgham Bilaghunnah", "Iqlab", "Ikhfa' Haqiqi", "Izhar Halqi",
        "Ghunnah (Mushaddad)", "Tasydid",
        "Mad Thabi'i", "Mad Wajib Muttasil", "Mad Jaiz Munfasil", "Mad Badal",
        "Mad Aridh Lis-Sukun", "Mad Iwad",
        "Qalqalah", "Lam Jalalah Tafkhim", "Lam Jalalah Tarqiq",
        "Tafkhim (Huruf Isti'la)", "Tafkhim (Ra')", "Tarqiq (Ra')",
        "Waqaf Lazim", "Waqaf Laa", "Waqaf Jaiz", "Waqaf Wasl Aula", "Waqaf Waqaf Aula",
        "Waqaf Mu'anaqah",
    )

    /** Kategori "biasa" yang kurang menarik sebagai soal (prefer yang spesifik). */
    private val BORING_CATEGORIES = setOf(RuleCategory.SHADDAH, RuleCategory.SUKUN)

    /**
     * Pilih satu kata dari daftar kata ayat yang punya ≥1 hukum tajwid.
     * Target = hukum paling menarik (hindari tasydid generik). Null kalau
     * tidak ada kata ber-hukum di ayat itu.
     */
    fun pickWord(words: List<String>, random: Random = Random.Default): QuizQuestion? {
        val candidates = words.indices.filter { idx ->
            TajwidEngine.analyzeWord(words[idx], words.getOrNull(idx - 1), words.getOrNull(idx + 1))
                .isNotEmpty()
        }
        if (candidates.isEmpty()) return null

        val idx = candidates[random.nextInt(candidates.size)]
        return pickWordAt(words, idx, random)
    }

    /** Build a deterministic question for a chosen word/rule pair. */
    fun pickWordAt(
        words: List<String>,
        index: Int,
        random: Random = Random.Default,
        targetRuleName: String? = null,
    ): QuizQuestion? {
        val word = words.getOrNull(index) ?: return null
        val prev = words.getOrNull(index - 1)
        val next = words.getOrNull(index + 1)
        val rules = TajwidEngine.analyzeWord(word, prev, next)
        val target = targetRuleName?.let { name -> rules.firstOrNull { it.name == name } }
            ?: rules.firstOrNull { it.category !in BORING_CATEGORIES }
            ?: rules.firstOrNull()
            ?: return null
        // Pengecoh TIDAK boleh memuat hukum lain yang juga benar pada kata ini
        // (mis. "Mad Thabi'i" vs "Mad Aridh" di kata akhir ayat) — biar tak ada
        // dua jawaban yang sama-sama bisa dibela.
        return QuizQuestion(word, prev, next, target, buildOptions(target.name, random, rules.map { it.name }))
    }

    /** 4 opsi: jawaban benar + 3 pengecoh acak (diacak urutannya). */
    fun buildOptions(
        correct: String,
        random: Random = Random.Default,
        exclude: Collection<String> = emptyList(),
    ): List<String> {
        val distractors = DISTRACTOR_POOL
            .filter { it != correct && it !in exclude }
            .shuffled(random)
            .take(OPTION_COUNT - 1)
        return (distractors + correct).shuffled(random)
    }

    fun isCorrect(answer: String, question: QuizQuestion): Boolean =
        answer == question.targetRule.name
}
