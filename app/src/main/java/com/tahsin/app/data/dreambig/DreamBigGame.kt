package com.tahsin.app.data.dreambig

import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.data.vocab.VocabQuizQuestion
import com.tahsin.app.data.vocab.VocabularyEngine
import com.tahsin.app.util.AppLanguage
import kotlin.random.Random

/**
 * Aturan main game "Dream BIG" (mode arcade) — murni JVM, tanpa Context,
 * bisa di-unit-test. Satu ronde = 10 soal kuis kosakata yang DIACAK dari
 * seluruh kosakata terkurasi — tanpa level/unlock, bisa dimainkan terus.
 */
object DreamBigGame {

    /** Jumlah soal per ronde. */
    const val QUESTIONS_PER_ROUND = 10

    /** Ambil [count] target unik dari kolam (urutan acak via [random]). */
    fun pickTargets(pool: List<VocabEntry>, count: Int, random: Random): List<VocabEntry> =
        pool.shuffled(random).take(count)

    /**
     * Satu soal pilihan ganda (delegasi ke [VocabularyEngine.makeQuiz]).
     * @param reverse true → "kata mana yang artinya X" (opsi = kata).
     */
    fun question(
        pool: List<VocabEntry>,
        target: VocabEntry,
        lang: AppLanguage,
        reverse: Boolean,
        random: Random = Random.Default,
    ): VocabQuizQuestion? = VocabularyEngine.makeQuiz(pool, target, lang, reverse, random)

    /** Bintang: 3★ ≥ 80%, 2★ ≥ 60%, 1★ ≥ 40% dari total soal. */
    fun stars(score: Int, total: Int): Int {
        if (total <= 0 || score <= 0) return 0
        val pct = score * 100 / total
        return when {
            pct >= 80 -> 3
            pct >= 60 -> 2
            pct >= 40 -> 1
            else -> 0
        }
    }
}
