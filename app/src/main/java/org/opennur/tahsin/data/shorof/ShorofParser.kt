package org.opennur.tahsin.data.shorof

import com.google.gson.Gson

/** Parser defensif untuk aset kursus Shorof. */
object ShorofParser {
    private val gson = Gson()

    fun parse(json: String): ShorofCatalog {
        val root = runCatching { gson.fromJson(json, CatalogJson::class.java) }.getOrNull()
            ?: return ShorofCatalog(0, emptyList())
        return ShorofCatalog(
            schemaVersion = root.schemaVersion ?: 0,
            levels = root.levels.orEmpty().map { level ->
                ShorofLevel(
                    id = level.id,
                    titleId = level.titleId.orEmpty(),
                    titleEn = level.titleEn.orEmpty(),
                    titleAr = level.titleAr.orEmpty(),
                    lessons = level.lessons.orEmpty().map { lesson ->
                        ShorofLesson(
                            id = lesson.id.orEmpty(),
                            titleId = lesson.titleId.orEmpty(),
                            titleEn = lesson.titleEn.orEmpty(),
                            titleAr = lesson.titleAr.orEmpty(),
                            introId = lesson.introId.orEmpty(),
                            introEn = lesson.introEn.orEmpty(),
                            rules = lesson.rules.orEmpty().map { rule ->
                                ShorofRule(
                                    titleId = rule.titleId.orEmpty(),
                                    titleEn = rule.titleEn.orEmpty(),
                                    explanationId = rule.explanationId.orEmpty(),
                                    explanationEn = rule.explanationEn.orEmpty(),
                                    exampleAr = rule.exampleAr.orEmpty(),
                                    exampleLatin = rule.exampleLatin.orEmpty(),
                                    exampleId = rule.exampleId.orEmpty(),
                                    exampleEn = rule.exampleEn.orEmpty(),
                                )
                            },
                            patterns = lesson.patterns.orEmpty().map { pattern ->
                                ShorofPattern(
                                    root = pattern.root.orEmpty(),
                                    rootLatin = pattern.rootLatin.orEmpty(),
                                    wazan = pattern.wazan.orEmpty(),
                                    wazanLatin = pattern.wazanLatin.orEmpty(),
                                    formId = pattern.formId.orEmpty(),
                                    formEn = pattern.formEn.orEmpty(),
                                    meaningId = pattern.meaningId.orEmpty(),
                                    meaningEn = pattern.meaningEn.orEmpty(),
                                    exampleAr = pattern.exampleAr.orEmpty(),
                                    exampleLatin = pattern.exampleLatin.orEmpty(),
                                )
                            },
                            conjugations = lesson.conjugations.orEmpty().map { row ->
                                ShorofConjugation(
                                    pronounAr = row.pronounAr.orEmpty(),
                                    pronounLatin = row.pronounLatin.orEmpty(),
                                    past = row.past.orEmpty(),
                                    present = row.present.orEmpty(),
                                    imperative = row.imperative.orEmpty(),
                                )
                            },
                            exercises = lesson.exercises.orEmpty().mapNotNull { it.toExercise() },
                        )
                    },
                )
            },
        )
    }

    private data class CatalogJson(val schemaVersion: Int? = null, val levels: List<LevelJson>? = null)

    private data class LevelJson(
        val id: Int = 0,
        val titleId: String? = null,
        val titleEn: String? = null,
        val titleAr: String? = null,
        val lessons: List<LessonJson>? = null,
    )

    private data class LessonJson(
        val id: String? = null,
        val titleId: String? = null,
        val titleEn: String? = null,
        val titleAr: String? = null,
        val introId: String? = null,
        val introEn: String? = null,
        val rules: List<RuleJson>? = null,
        val patterns: List<PatternJson>? = null,
        val conjugations: List<ConjugationJson>? = null,
        val exercises: List<ExerciseJson>? = null,
    )

    private data class RuleJson(
        val titleId: String? = null,
        val titleEn: String? = null,
        val explanationId: String? = null,
        val explanationEn: String? = null,
        val exampleAr: String? = null,
        val exampleLatin: String? = null,
        val exampleId: String? = null,
        val exampleEn: String? = null,
    )

    private data class PatternJson(
        val root: String? = null,
        val rootLatin: String? = null,
        val wazan: String? = null,
        val wazanLatin: String? = null,
        val formId: String? = null,
        val formEn: String? = null,
        val meaningId: String? = null,
        val meaningEn: String? = null,
        val exampleAr: String? = null,
        val exampleLatin: String? = null,
    )

    private data class ConjugationJson(
        val pronounAr: String? = null,
        val pronounLatin: String? = null,
        val past: String? = null,
        val present: String? = null,
        val imperative: String? = null,
    )

    private data class ExerciseJson(
        val type: String? = null,
        val promptId: String? = null,
        val promptEn: String? = null,
        val promptAr: String? = null,
        val promptLatin: String? = null,
        val optionsId: List<String>? = null,
        val optionsEn: List<String>? = null,
        val answerIndex: Int? = null,
    ) {
        fun toExercise(): ShorofExercise? {
            if (type != "choice") return null
            val id = optionsId.orEmpty()
            val en = optionsEn.orEmpty()
            val answer = answerIndex ?: return null
            if (id.size < 3 || id.size != en.size || answer !in id.indices) return null
            return ShorofChoiceExercise(
                promptId = promptId.orEmpty(),
                promptEn = promptEn.orEmpty(),
                promptAr = promptAr.orEmpty(),
                promptLatin = promptLatin.orEmpty(),
                optionsId = id,
                optionsEn = en,
                answerIndex = answer,
            )
        }
    }
}
