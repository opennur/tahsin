package com.tahsin.app.data.lughoh

import com.tahsin.app.util.ArabicNormalizer
import kotlin.random.Random

/**
 * Logika murni latihan (tadribat) — tanpa Android/Context, bisa di-unit-test.
 * Pemeriksaan jawaban memakai [ArabicNormalizer.normalize] (buang harakat,
 * samakan hamza/alif) agar cocok dengan normalisasi `tools/build_lughoh.py`.
 */
object LughohEngine {

    /** Jumlah latihan per sesi acak (arcade). */
    const val SESSION_SIZE = 8

    /**
     * Sesi latihan acak (arcade): ambil [count] latihan unik dari SELURUH
     * pelajaran lalu acak urutan opsi pilihan ganda agar posisi tidak dihafal.
     */
    fun buildRandomSession(
        lessons: List<LughohLesson>,
        count: Int,
        random: Random = Random.Default,
    ): List<Exercise> {
        val all = lessons.flatMap { it.tadribat }
        if (all.isEmpty()) return emptyList()
        return all.shuffled(random).take(count).map { shuffleOptions(it, random) }
    }

    /** Salin latihan pilihan ganda dengan urutan opsi diacak (jawaban tetap). */
    fun shuffleOptions(exercise: Exercise, random: Random): Exercise = when (exercise) {
        is FillBlankExercise -> exercise.copy(options = exercise.options.shuffled(random))
        is TranslateArIdExercise -> exercise.copy(options = exercise.options.shuffled(random))
        is TranslateIdArExercise -> exercise.copy(options = exercise.options.shuffled(random))
        is RearrangeExercise -> exercise // kata diacak saat tampil (shuffleRearrange)
    }

    /** Apakah pilihan [selected] jawaban benar untuk latihan pilihan. */
    fun isChoiceCorrect(exercise: Exercise, selected: String): Boolean {
        val answer = when (exercise) {
            is FillBlankExercise -> exercise.answer
            is TranslateArIdExercise -> exercise.answer
            is TranslateIdArExercise -> exercise.answer
            is RearrangeExercise -> return false // bukan latihan pilihan
        }
        return normalizeChoice(answer) == normalizeChoice(selected)
    }

    /**
     * Susunan tampilan latihan menyusun kata: acak [exercise.words] tetapi
     * TIDAK boleh sama dengan urutan benar (kalau cuma 1 kata, biarkan).
     */
    fun shuffleRearrange(exercise: RearrangeExercise, random: Random): List<WordChip> {
        val words = exercise.words
        if (words.size <= 1) return words
        var order = words.indices.shuffled(random)
        var guard = 0
        while (order == words.indices.toList() && guard < 10) {
            order = words.indices.shuffled(random)
            guard++
        }
        return order.map { words[it] }
    }

    /** Apakah urutan kata yang diketuk [tapped] membentuk kalimat benar. */
    fun isRearrangeCorrect(exercise: RearrangeExercise, tapped: List<WordChip>): Boolean {
        if (tapped.size != exercise.answer.size) return false
        return tapped.zip(exercise.answer).all { (chip, expected) ->
            ArabicNormalizer.normalize(chip.ar) == ArabicNormalizer.normalize(expected)
        }
    }

    /**
     * Apakah kata [chip] pada posisi [position] urutan jawaban benar.
     * Dipakai UI untuk mewarnai per-kata saat jawaban diperiksa.
     */
    fun isChipAtPositionCorrect(
        exercise: RearrangeExercise,
        chip: WordChip,
        position: Int,
    ): Boolean {
        val expected = exercise.answer.getOrNull(position) ?: return false
        return ArabicNormalizer.normalize(chip.ar) == ArabicNormalizer.normalize(expected)
    }

    private fun normalizeChoice(value: String): String =
        ArabicNormalizer.normalize(value)
}
