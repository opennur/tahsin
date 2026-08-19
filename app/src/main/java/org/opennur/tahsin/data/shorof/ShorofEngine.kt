package org.opennur.tahsin.data.shorof

import kotlin.random.Random

/** Logika murni pemilihan dan penilaian latihan Shorof. */
object ShorofEngine {
    const val SESSION_SIZE = 8

    fun buildSession(
        lessons: List<ShorofLesson>,
        count: Int,
        random: Random = Random.Default,
        allowedIds: Set<String>? = null,
    ): List<ShorofSessionExercise> = lessons
        .flatMap { lesson -> lesson.exercises.mapIndexed { index, exercise ->
            val id = questionId(lesson.id, index)
            ShorofSessionExercise(lesson.id, exercise, id)
        } }
        .filter { allowedIds == null || it.questionId in allowedIds }
        .shuffled(random)
        .take(count)

    fun allQuestionIds(lessons: List<ShorofLesson>): List<String> = lessons.flatMap { lesson ->
        lesson.exercises.indices.map { index -> questionId(lesson.id, index) }
    }

    fun questionId(lessonId: String, index: Int): String = "$lessonId:$index"

    fun isChoiceCorrect(exercise: ShorofChoiceExercise, selectedIndex: Int): Boolean =
        selectedIndex == exercise.answerIndex
}
