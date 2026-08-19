package org.opennur.tahsin.data.nahwu

import org.opennur.tahsin.util.ArabicNormalizer
import kotlin.random.Random

/** Logika murni pemilihan dan penilaian latihan Nahwu. */
object NahwuEngine {
    const val SESSION_SIZE = 8

    fun buildSession(
        lessons: List<NahwuLesson>,
        count: Int,
        random: Random = Random.Default,
        allowedIds: Set<String>? = null,
    ): List<NahwuSessionExercise> = lessons
        .flatMap { lesson -> lesson.exercises.mapIndexed { index, exercise ->
            val id = questionId(lesson.id, index)
            NahwuSessionExercise(lesson.id, exercise, id)
        } }
        .filter { allowedIds == null || it.questionId in allowedIds }
        .shuffled(random)
        .take(count)

    fun allQuestionIds(lessons: List<NahwuLesson>): List<String> = lessons.flatMap { lesson ->
        lesson.exercises.indices.map { index -> questionId(lesson.id, index) }
    }

    fun questionId(lessonId: String, index: Int): String = "$lessonId:$index"

    fun shuffleWords(exercise: NahwuRearrangeExercise, random: Random = Random.Default): List<NahwuWord> {
        if (exercise.words.size < 2) return exercise.words
        var result = exercise.words.shuffled(random)
        var guard = 0
        while (result == exercise.words && guard++ < 10) result = exercise.words.shuffled(random)
        return result
    }

    fun isChoiceCorrect(exercise: NahwuChoiceExercise, selectedIndex: Int): Boolean =
        selectedIndex == exercise.answerIndex

    fun isRearrangeCorrect(
        exercise: NahwuRearrangeExercise,
        shown: List<NahwuWord>,
        selectedIndices: List<Int>,
    ): Boolean {
        if (selectedIndices.size != exercise.words.size) return false
        return selectedIndices.mapNotNull { shown.getOrNull(it)?.ar }
            .map(ArabicNormalizer::normalize) == exercise.words.map { ArabicNormalizer.normalize(it.ar) }
    }
}
