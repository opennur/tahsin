package org.opennur.tahsin.data.shorof

/** Katalog kursus Shorof offline. */
data class ShorofCatalog(
    val schemaVersion: Int,
    val levels: List<ShorofLevel>,
)

data class ShorofLevel(
    val id: Int,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val lessons: List<ShorofLesson>,
)

data class ShorofLesson(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val titleAr: String,
    val introId: String,
    val introEn: String,
    val rules: List<ShorofRule>,
    val patterns: List<ShorofPattern>,
    val conjugations: List<ShorofConjugation>,
    val exercises: List<ShorofExercise>,
)

data class ShorofRule(
    val titleId: String,
    val titleEn: String,
    val explanationId: String,
    val explanationEn: String,
    val exampleAr: String,
    val exampleLatin: String,
    val exampleId: String,
    val exampleEn: String,
)

data class ShorofPattern(
    val root: String,
    val rootLatin: String,
    val wazan: String,
    val wazanLatin: String,
    val formId: String,
    val formEn: String,
    val meaningId: String,
    val meaningEn: String,
    val exampleAr: String,
    val exampleLatin: String,
)

data class ShorofConjugation(
    val pronounAr: String,
    val pronounLatin: String,
    val past: String,
    val present: String,
    val imperative: String,
)

sealed interface ShorofExercise {
    val promptId: String
    val promptEn: String
    val promptAr: String
    val promptLatin: String
}

data class ShorofChoiceExercise(
    override val promptId: String,
    override val promptEn: String,
    override val promptAr: String,
    override val promptLatin: String,
    val optionsId: List<String>,
    val optionsEn: List<String>,
    val answerIndex: Int,
) : ShorofExercise

data class ShorofSessionExercise(
    val lessonId: String,
    val exercise: ShorofExercise,
    val questionId: String = "",
)
