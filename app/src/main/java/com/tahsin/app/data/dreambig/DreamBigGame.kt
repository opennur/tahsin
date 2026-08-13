package com.tahsin.app.data.dreambig

import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.data.vocab.VocabQuizQuestion
import com.tahsin.app.data.vocab.VocabularyEngine
import com.tahsin.app.util.AppLanguage
import kotlin.random.Random

/**
 * Aturan main game "Dream BIG" — murni JVM, tanpa Context, bisa di-unit-test.
 * Satu level = satu ronde kuis kosakata (10 soal) dari kolam kata level itu.
 */
object DreamBigGame {

    /** Jumlah soal per ronde. */
    const val QUESTIONS_PER_LEVEL = 10

    /** Skor minimum untuk dianggap lulus (bintang ≥ 1). */
    const val PASS_SCORE = 4

    /** Petakan kunci level → entri terkurasi (kunci hilang/arti kosong dilewati). */
    fun wordsFor(level: DreamBigLevel, entries: List<VocabEntry>): List<VocabEntry> {
        val byKey = entries.associateBy { it.key }
        return level.wordKeys.mapNotNull { key ->
            byKey[key]?.takeIf { it.meaningId.isNotBlank() && it.meaningEn.isNotBlank() }
        }
    }

    /** Ambil [count] target unik dari kolam (urutan acak deterministik via [random]). */
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

    /**
     * Level [day] terbuka bila day == 1, atau day-1 sudah selesai
     * (ada di [completedDays]).
     */
    fun unlocked(day: Int, completedDays: Set<Int>): Boolean =
        day <= 1 || (day - 1) in completedDays
}
