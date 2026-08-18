package org.opennur.tahsin.data.nahwu

import com.google.gson.Gson

/** Parser defensif untuk aset kursus Nahwu. */
object NahwuParser {
    private val gson = Gson()

    fun parse(json: String): NahwuCatalog {
        val root = runCatching { gson.fromJson(json, CatalogJson::class.java) }.getOrNull()
            ?: return NahwuCatalog(schemaVersion = 0, levels = emptyList())
        return NahwuCatalog(
            schemaVersion = root.schemaVersion ?: 0,
            levels = root.levels.orEmpty().map { level ->
                NahwuLevel(
                    id = level.id,
                    titleId = level.titleId.orEmpty(),
                    titleEn = level.titleEn.orEmpty(),
                    titleAr = level.titleAr.orEmpty(),
                    lessons = level.lessons.orEmpty().map { lesson ->
                        NahwuLesson(
                            id = lesson.id.orEmpty(),
                            titleId = lesson.titleId.orEmpty(),
                            titleEn = lesson.titleEn.orEmpty(),
                            titleAr = lesson.titleAr.orEmpty(),
                            introId = lesson.introId.orEmpty(),
                            introEn = lesson.introEn.orEmpty(),
                            rules = lesson.rules.orEmpty().map { rule ->
                                NahwuRule(
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
                            exercises = lesson.exercises.orEmpty().mapNotNull { it.toExercise() },
                        )
                    },
                )
            },
        )
    }

    private data class CatalogJson(
        val schemaVersion: Int? = null,
        val levels: List<LevelJson>? = null,
    )

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

    private data class ExerciseJson(
        val type: String? = null,
        val promptId: String? = null,
        val promptEn: String? = null,
        val promptAr: String? = null,
        val promptLatin: String? = null,
        val optionsId: List<String>? = null,
        val optionsEn: List<String>? = null,
        val answerIndex: Int? = null,
        val words: List<WordJson>? = null,
    ) {
        fun toExercise(): NahwuExercise? {
            return when (type) {
            "choice" -> {
                val id = optionsId.orEmpty()
                val en = optionsEn.orEmpty()
                val answer = answerIndex ?: return null
                if (id.size != en.size || answer !in id.indices) return null
                NahwuChoiceExercise(
                    promptId = promptId.orEmpty(),
                    promptEn = promptEn.orEmpty(),
                    promptAr = promptAr.orEmpty(),
                    promptLatin = promptLatin.orEmpty(),
                    optionsId = id,
                    optionsEn = en,
                    answerIndex = answer,
                )
            }
            "rearrange" -> {
                val values = words.orEmpty().map { NahwuWord(it.ar.orEmpty(), it.latin.orEmpty()) }
                if (values.size < 2) null else NahwuRearrangeExercise(
                    promptId = promptId.orEmpty(),
                    promptEn = promptEn.orEmpty(),
                    words = values,
                )
            }
            else -> null
        }
        }
    }

    private data class WordJson(
        val ar: String? = null,
        val latin: String? = null,
    )
}
