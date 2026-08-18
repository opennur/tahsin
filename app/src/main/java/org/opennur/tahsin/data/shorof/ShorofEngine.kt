package org.opennur.tahsin.data.shorof

import kotlin.random.Random

/** Logika murni pemilihan dan penilaian latihan Shorof. */
object ShorofEngine {
    const val SESSION_SIZE = 8

    fun buildSession(
        lessons: List<ShorofLesson>,
        count: Int,
        random: Random = Random.Default,
    ): List<ShorofSessionExercise> = lessons
        .flatMap { lesson -> lesson.exercises.map { ShorofSessionExercise(lesson.id, it) } }
        .shuffled(random)
        .take(count)

    fun isChoiceCorrect(exercise: ShorofChoiceExercise, selectedIndex: Int): Boolean =
        selectedIndex == exercise.answerIndex
}
