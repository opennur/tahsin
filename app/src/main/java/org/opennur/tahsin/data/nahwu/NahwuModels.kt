package org.opennur.tahsin.data.nahwu

/** Katalog kursus Nahwu offline. */
data class NahwuCatalog(
    val schemaVersion: Int,
    val levels: List<NahwuLevel>,
)

data class NahwuLevel(
    val id: Int,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val lessons: List<NahwuLesson>,
)

data class NahwuLesson(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val introId: String,
    val introEn: String,
    val rules: List<NahwuRule>,
    val exercises: List<NahwuExercise>,
)

data class NahwuRule(
    val titleId: String,
    val titleEn: String,
    val explanationId: String,
    val explanationEn: String,
    val exampleAr: String,
    val exampleLatin: String,
    val exampleId: String,
    val exampleEn: String,
)

data class NahwuWord(
    val ar: String,
    val latin: String,
)

sealed interface NahwuExercise {
    val promptId: String
    val promptEn: String
}

data class NahwuChoiceExercise(
    override val promptId: String,
    override val promptEn: String,
    val promptAr: String,
    val promptLatin: String,
    val optionsId: List<String>,
    val optionsEn: List<String>,
    val answerIndex: Int,
) : NahwuExercise

data class NahwuRearrangeExercise(
    override val promptId: String,
    override val promptEn: String,
    val words: List<NahwuWord>,
) : NahwuExercise

data class NahwuSessionExercise(
    val lessonId: String,
    val exercise: NahwuExercise,
)
