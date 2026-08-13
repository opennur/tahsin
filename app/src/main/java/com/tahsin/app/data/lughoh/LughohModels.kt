package com.tahsin.app.data.lughoh

/**
 * Katalog pelajaran Bahasa Arab "Belajar Arab" — hasil `tools/build_lughoh.py`
 * → `assets/lughoh/lessons.json`. Konten orisinal, metodologi ala Durusul
 * Lughoh (Muhadatsah → Mufrodat → Qawa'id → Tadribat).
 */
data class LughohCatalog(
    val schemaVersion: Int,
    val levels: List<LughohLevel>,
)

/** Satu level (1..3): tema besar + daftar pelajaran. */
data class LughohLevel(
    val id: Int,
    val titleId: String,
    val titleAr: String,
    val lessons: List<LughohLesson>,
)

/** Satu pelajaran: dialog, kosakata, tata bahasa, dan latihan. */
data class LughohLesson(
    val id: String,
    val titleId: String,
    val titleAr: String,
    val muhadatsah: List<DialogueLine>,
    val mufrodat: List<VocabWord>,
    val qawaid: List<GrammarRule>,
    val tadribat: List<Exercise>,
)

/** Satu baris dialog: pembicara + Arab berharakat + transliterasi + arti ID. */
data class DialogueLine(
    val speaker: String,
    val ar: String,
    val latin: String,
    val id: String,
)

/** Satu kata kosakata baru (dari percakapan) + contoh kalimat. */
data class VocabWord(
    val ar: String,
    val latin: String,
    val id: String,
    val exampleAr: String,
    val exampleLatin: String,
    val exampleId: String,
)

/** Satu kaidah tata bahasa; contohnya selalu diambil dari dialog. */
data class GrammarRule(
    val titleId: String,
    val id: String,
    val exampleAr: String,
    val exampleLatin: String,
    val exampleId: String,
)

/** Jenis latihan (tadribat) — semuanya tap-based. */
enum class ExerciseType { FILL_BLANK, TRANSLATE_AR_ID, TRANSLATE_ID_AR, REARRANGE }

/** Latihan pilihan/ketuk. [type] menentukan UI dan cara memeriksa jawaban. */
sealed interface Exercise {
    val type: ExerciseType
}

/** Isi titik-titik: pilih satu kata Arab untuk mengisi "____" pada kalimat. */
data class FillBlankExercise(
    val promptId: String,
    val promptAr: String,
    val promptLatin: String,
    val options: List<String>,
    val answer: String,
) : Exercise {
    override val type: ExerciseType = ExerciseType.FILL_BLANK

    /** Kalimat tampilan: "____" diganti placeholder agar bisa digayakan. */
    val displayPromptAr: String get() = promptAr.replace("____", "⋯⋯")
    val displayPromptLatin: String get() = promptLatin.replace("____", "⋯⋯")
}

/** Terjemahkan Arab → Indonesia: pilih terjemahan yang benar. */
data class TranslateArIdExercise(
    val promptAr: String,
    val promptLatin: String,
    val options: List<String>,
    val answer: String,
) : Exercise {
    override val type: ExerciseType = ExerciseType.TRANSLATE_AR_ID
}

/** Terjemahkan Indonesia → Arab: pilih kalimat Arab yang benar. */
data class TranslateIdArExercise(
    val promptId: String,
    val options: List<String>,
    val answer: String,
) : Exercise {
    override val type: ExerciseType = ExerciseType.TRANSLATE_ID_AR
}

/** Satu kata dalam latihan menyusun kalimat. */
data class WordChip(
    val ar: String,
    val latin: String,
)

/**
 * Susun kata menjadi kalimat yang benar: [words] tersimpan URUTAN BENAR;
 * UI mengacaknya saat tampil, jawaban [answer] = urutan `ar` dari [words].
 */
data class RearrangeExercise(
    val words: List<WordChip>,
    val answer: List<String>,
) : Exercise {
    override val type: ExerciseType = ExerciseType.REARRANGE
}
