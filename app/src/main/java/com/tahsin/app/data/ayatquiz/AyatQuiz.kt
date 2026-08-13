package com.tahsin.app.data.ayatquiz

import com.tahsin.app.util.ArabicNormalizer
import kotlin.random.Random

/** Satu soal "Lengkapi Ayat": ayat dengan satu kata kosong + 4 opsi kata. */
data class AyatQuizQuestion(
    val surahNumber: Int,
    val ayahNumber: Int,
    /** Teks ayat dengan kata target diganti [AyatQuiz.BLANK]. */
    val blankedText: String,
    val correctWord: String,
    val options: List<String>,
)

/**
 * Generator soal "Lengkapi Ayat" (kata mana yang melengkapi ayat ini?) —
 * murni, tanpa Android, bisa diuji JVM. Memakai kata-kata ayat itu sendiri
 * untuk konteks dan kolam kata (biasanya semua kata dalam surah yang sama)
 * untuk pengecoh; kecocokan jawaban memakai [ArabicNormalizer] supaya
 * perbedaan harakat tidak bikin ambigu.
 */
object AyatQuiz {

    const val OPTION_COUNT = 4
    const val BLANK = "…"

    /**
     * Buat soal dari kata-kata satu ayat + kolam pengecoh. Target dipilih
     * dari TENGAH ayat (bukan kata pertama/terakhir) supaya konteks
     * kiri-kanannya cukup. Null kalau ayat terlalu pendek (< 3 kata) atau
     * kolam pengecoh terlalu sedikit.
     */
    fun makeQuestion(
        surahNumber: Int,
        ayahNumber: Int,
        words: List<String>,
        pool: List<String>,
        random: Random = Random.Default,
    ): AyatQuizQuestion? {
        if (words.size < 3) return null
        val targetIndex = 1 + random.nextInt(words.size - 2)
        val correct = words[targetIndex]
        val normalizedCorrect = ArabicNormalizer.normalize(correct)
        // Pengecoh: kata lain yang bukan bentuk normalisasi sama dengan target
        // (hindari dua opsi "sama" yang hanya beda harakat → soal ambigu).
        val candidates = pool
            .map { it.trim() }
            .filter { it.isNotBlank() && ArabicNormalizer.normalize(it) != normalizedCorrect }
            .distinct()
        if (candidates.size < OPTION_COUNT - 1) return null

        val distractors = candidates.shuffled(random).take(OPTION_COUNT - 1)
        val context = words.toMutableList()
        context[targetIndex] = BLANK
        return AyatQuizQuestion(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            blankedText = context.joinToString(" "),
            correctWord = correct,
            options = (distractors + correct).shuffled(random),
        )
    }

    fun isCorrect(answer: String, question: AyatQuizQuestion): Boolean =
        ArabicNormalizer.normalize(answer) == ArabicNormalizer.normalize(question.correctWord)
}
