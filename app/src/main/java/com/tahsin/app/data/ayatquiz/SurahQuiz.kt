package com.tahsin.app.data.ayatquiz

import kotlin.random.Random

/** Satu soal "Tebak Surah": potongan ayat + 4 opsi nama surah. */
data class SurahQuizQuestion(
    val surahNumber: Int,
    val ayahNumber: Int,
    /** Potongan ayat (beberapa kata pertama). */
    val fragment: String,
    val correctName: String,
    val options: List<String>,
)

/**
 * Generator soal "Tebak Surah" (ayat ini dari surah apa?) — murni, tanpa
 * Android. Memakai potongan ayat (FRAGMENT_WORDS kata pertama) sebagai
 * petunjuk dan nama latin surah sebagai opsi.
 */
object SurahQuiz {

    const val OPTION_COUNT = 4
    const val FRAGMENT_WORDS = 5

    /**
     * Buat soal dari satu ayat + daftar nama surah. Null kalau nama surah
     * tidak dikenal atau kolam pengecoh kurang (jarang — ada 114 surah).
     */
    fun makeQuestion(
        surahNumber: Int,
        ayahNumber: Int,
        arabic: String,
        surahNames: List<Pair<Int, String>>,
        random: Random = Random.Default,
    ): SurahQuizQuestion? {
        val match = surahNames.firstOrNull { it.first == surahNumber }
            ?: return null
        val correctName = match.second
        val distractorNames = surahNames
            .filter { it.first != surahNumber }
            .map { it.second }
            .filter { it.isNotBlank() }
            .distinct()
        if (distractorNames.size < OPTION_COUNT - 1) return null

        val fragment = arabic
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(FRAGMENT_WORDS)
            .joinToString(" ")
        return SurahQuizQuestion(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            fragment = fragment,
            correctName = correctName,
            options = (distractorNames.shuffled(random).take(OPTION_COUNT - 1) + correctName)
                .shuffled(random),
        )
    }

    fun isCorrect(answer: String, question: SurahQuizQuestion): Boolean =
        answer == question.correctName
}
