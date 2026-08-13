package com.tahsin.app.data.lughoh

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Parsing `assets/lughoh/lessons.json` — murni JVM, tanpa Context, bisa
 * di-unit-test. I/O (assets) ditangani [LughohRepository]; parser ini hanya
 * JSON → model.
 *
 * DTO memakai field nullable: Gson tidak memanggil konstruktor (unsafe
 * allocation), jadi field yang hilang bernilai null — mapping eksplisit agar
 * model tidak pernah membawa null. JSON rusak/kosong → katalog kosong.
 */
object LughohParser {

    private val gson = Gson()

    fun parse(json: String): LughohCatalog {
        val parsed = runCatching { gson.fromJson(json, CatalogJson::class.java) }.getOrNull()
            ?: return LughohCatalog(schemaVersion = 0, levels = emptyList())
        return LughohCatalog(
            schemaVersion = parsed.schemaVersion ?: 0,
            levels = parsed.levels.orEmpty().map { it.toLevel() },
        )
    }

    private data class CatalogJson(
        val schemaVersion: Int? = null,
        val levels: List<LevelJson>? = null,
    )

    private data class LevelJson(
        val id: Int = 0,
        val titleId: String? = null,
        val titleAr: String? = null,
        val lessons: List<LessonJson>? = null,
    ) {
        fun toLevel() = LughohLevel(
            id = id,
            titleId = titleId.orEmpty(),
            titleAr = titleAr.orEmpty(),
            lessons = lessons.orEmpty().map { it.toLesson() },
        )
    }

    private data class LessonJson(
        val id: String? = null,
        val titleId: String? = null,
        val titleAr: String? = null,
        val muhadatsah: List<LineJson>? = null,
        val mufrodat: List<VocabJson>? = null,
        val qawaid: List<GrammarJson>? = null,
        val tadribat: List<ExerciseJson>? = null,
    ) {
        fun toLesson() = LughohLesson(
            id = id.orEmpty(),
            titleId = titleId.orEmpty(),
            titleAr = titleAr.orEmpty(),
            muhadatsah = muhadatsah.orEmpty().map { it.toLine() },
            mufrodat = mufrodat.orEmpty().map { it.toVocab() },
            qawaid = qawaid.orEmpty().map { it.toRule() },
            tadribat = tadribat.orEmpty().mapNotNull { it.toExercise() },
        )
    }

    private data class LineJson(
        val speaker: String? = null,
        val ar: String? = null,
        val latin: String? = null,
        val id: String? = null,
    ) {
        fun toLine() = DialogueLine(
            speaker = speaker.orEmpty(),
            ar = ar.orEmpty(),
            latin = latin.orEmpty(),
            id = id.orEmpty(),
        )
    }

    private data class VocabJson(
        val ar: String? = null,
        val latin: String? = null,
        val id: String? = null,
        val exampleAr: String? = null,
        val exampleLatin: String? = null,
        val exampleId: String? = null,
    ) {
        fun toVocab() = VocabWord(
            ar = ar.orEmpty(),
            latin = latin.orEmpty(),
            id = id.orEmpty(),
            exampleAr = exampleAr.orEmpty(),
            exampleLatin = exampleLatin.orEmpty(),
            exampleId = exampleId.orEmpty(),
        )
    }

    private data class GrammarJson(
        val titleId: String? = null,
        val id: String? = null,
        val exampleAr: String? = null,
        val exampleLatin: String? = null,
        val exampleId: String? = null,
    ) {
        fun toRule() = GrammarRule(
            titleId = titleId.orEmpty(),
            id = id.orEmpty(),
            exampleAr = exampleAr.orEmpty(),
            exampleLatin = exampleLatin.orEmpty(),
            exampleId = exampleId.orEmpty(),
        )
    }

    private data class WordChipJson(
        val ar: String? = null,
        val latin: String? = null,
    ) {
        fun toChip() = WordChip(ar = ar.orEmpty(), latin = latin.orEmpty())
    }

    private data class ExerciseJson(
        val type: String? = null,
        val promptId: String? = null,
        val promptAr: String? = null,
        val promptLatin: String? = null,
        val options: List<String>? = null,
        val answer: JsonElement? = null,
        val words: List<WordChipJson>? = null,
    ) {
        /**
         * Satu latihan rusak (tipe `answer` tidak cocok, JSON tak terduga,
         * dll.) → di-skip, bukan crash. Catatan: `JsonElement.asString`
         * Gson 2.10.1 menerima array berisi SATU elemen, jadi tipe dicek
         * eksplisit (isJsonPrimitive / isJsonArray) agar disiplin.
         */
        fun toExercise(): Exercise? = runCatching {
            when (type) {
                "fillBlank" -> {
                    val ans = answer?.asStringOrNull() ?: return@runCatching null
                    FillBlankExercise(
                        promptId = promptId.orEmpty(),
                        promptAr = promptAr.orEmpty(),
                        promptLatin = promptLatin.orEmpty(),
                        options = options.orEmpty(),
                        answer = ans,
                    )
                }
                "translateArId" -> {
                    val ans = answer?.asStringOrNull() ?: return@runCatching null
                    TranslateArIdExercise(
                        promptAr = promptAr.orEmpty(),
                        promptLatin = promptLatin.orEmpty(),
                        options = options.orEmpty(),
                        answer = ans,
                    )
                }
                "translateIdAr" -> {
                    val ans = answer?.asStringOrNull() ?: return@runCatching null
                    TranslateIdArExercise(
                        promptId = promptId.orEmpty(),
                        options = options.orEmpty(),
                        answer = ans,
                    )
                }
                "rearrange" -> {
                    if (answer == null || !answer.isJsonArray) return@runCatching null
                    RearrangeExercise(
                        words = words.orEmpty().map { it.toChip() },
                        answer = answer.asJsonArray.mapNotNull { it.asString },
                    )
                }
                else -> null // jenis tak dikenal: di-skip
            }
        }.getOrNull()
    }

    /** String hanya jika benar-benar primitif (bukan array/objek). */
    private fun JsonElement?.asStringOrNull(): String? =
        this?.takeIf { it.isJsonPrimitive }?.asString
}
