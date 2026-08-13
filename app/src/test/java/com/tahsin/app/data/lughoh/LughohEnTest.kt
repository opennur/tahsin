package com.tahsin.app.data.lughoh

import com.tahsin.app.data.lughoh.LughohEngine.forLanguage
import com.tahsin.app.util.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.text.Regex

/**
 * Terjemahan Inggris konten "Belajar Arab":
 * 1. [LughohEngine.forLanguage] — resolusi latihan ke bahasa (ID/EN, fallback).
 * 2. Parser — field EN dibaca dari JSON; tanpa field → fallback kosong.
 * 3. INTEGRITAS KONTEN — file `assets/lughoh/lessons.json` ASLI: SEMUA teks
 *    Indonesia wajib punya terjemahan EN (kalau ada yang kosong, tes gagal —
 *    "kesalahan satu kata pun fatal" untuk materi kitab suci terkait).
 */
class LughohEnTest {

    // ------------------------------------------------------------------
    // forLanguage — FillBlank
    // ------------------------------------------------------------------

    private val fill = FillBlankExercise(
        promptId = "Isilah titik-titik: Apa ___?",
        promptAr = "مَا ____؟",
        promptLatin = "mā ____?",
        options = listOf("اسْمُكَ", "اسْمُكِ"),
        answer = "اسْمُكَ",
        promptEn = "Fill in the blank: What ___?",
    )

    @Test
    fun `fillBlank - ID tidak diubah`() {
        assertEquals(fill, fill.forLanguage(AppLanguage.ID))
    }

    @Test
    fun `fillBlank - EN memakai promptEn sebagai promptId`() {
        val resolved = fill.forLanguage(AppLanguage.EN) as FillBlankExercise
        assertEquals("Fill in the blank: What ___?", resolved.promptId)
        assertEquals(fill.promptAr, resolved.promptAr)
        assertEquals(fill.options, resolved.options)
    }

    @Test
    fun `fillBlank - EN tanpa promptEn fallback ke ID`() {
        val noEn = fill.copy(promptEn = "")
        assertEquals(noEn, noEn.forLanguage(AppLanguage.EN))
    }

    @Test
    fun `fillBlank - isChoiceCorrect berlaku di resolusi EN`() {
        val resolved = fill.forLanguage(AppLanguage.EN)
        assertTrue(LughohEngine.isChoiceCorrect(resolved, fill.answer))
    }
    // ------------------------------------------------------------------
    // forLanguage — TranslateArId
    // ------------------------------------------------------------------

    private val arId = TranslateArIdExercise(
        promptAr = "مَا اسْمُكِ؟",
        promptLatin = "mā smuki?",
        options = listOf("Siapa namamu?", "Apa ini?", "Dari mana kamu?"),
        answer = "Siapa namamu?",
        optionsEn = listOf("What is your name?", "What is this?", "Where are you from?"),
        answerEn = "What is your name?",
    )

    @Test
    fun `translateArId - ID tidak diubah`() {
        assertEquals(arId, arId.forLanguage(AppLanguage.ID))
    }

    @Test
    fun `translateArId - EN memakai optionsEn shuffled dan answerEn`() {
        val resolved = arId.forLanguage(AppLanguage.EN, Random(7)) as TranslateArIdExercise
        assertEquals(arId.optionsEn.toSet(), resolved.options.toSet())
        assertEquals("What is your name?", resolved.answer)
        assertTrue(LughohEngine.isChoiceCorrect(resolved, "What is your name?"))
        assertFalse(LughohEngine.isChoiceCorrect(resolved, "What is this?"))
    }

    @Test
    fun `translateArId - EN tanpa optionsEn fallback ke ID`() {
        val noEn = arId.copy(optionsEn = emptyList(), answerEn = "")
        assertEquals(noEn, noEn.forLanguage(AppLanguage.EN))
    }

    @Test
    fun `translateArId - EN dengan answerEn kosong fallback ke ID (tidak menebak)`() {
        val noAns = arId.copy(answerEn = "")
        assertEquals(noAns, noAns.forLanguage(AppLanguage.EN))
    }

    // ------------------------------------------------------------------
    // forLanguage — TranslateIdAr
    // ------------------------------------------------------------------

    private val idAr = TranslateIdArExercise(
        promptId = "Senang berkenalan.",
        options = listOf("أَهْلًا وَسَهْلًا", "مَرْحَبًا"),
        answer = "أَهْلًا وَسَهْلًا",
        promptEn = "Nice to meet you.",
    )

    @Test
    fun `translateIdAr - ID tidak diubah`() {
        assertEquals(idAr, idAr.forLanguage(AppLanguage.ID))
    }

    @Test
    fun `translateIdAr - EN memakai promptEn sebagai promptId`() {
        val resolved = idAr.forLanguage(AppLanguage.EN) as TranslateIdArExercise
        assertEquals("Nice to meet you.", resolved.promptId)
        assertEquals(idAr.options, resolved.options)
    }

    @Test
    fun `translateIdAr - EN tanpa promptEn fallback ke ID`() {
        val noEn = idAr.copy(promptEn = "")
        assertEquals(noEn, noEn.forLanguage(AppLanguage.EN))
    }

    // ------------------------------------------------------------------
    // forLanguage — Rearrange (netral)
    // ------------------------------------------------------------------

    private val rearrange = RearrangeExercise(
        words = listOf(WordChip("أَنَا", "ana"), WordChip("طَالِبٌ", "thālibun")),
        answer = listOf("أَنَا", "طَالِبٌ"),
    )

    @Test
    fun `rearrange - tidak berubah bahasa apapun`() {
        assertEquals(rearrange, rearrange.forLanguage(AppLanguage.ID))
        assertEquals(rearrange, rearrange.forLanguage(AppLanguage.EN))
    }

    // ------------------------------------------------------------------
    // Parser — field EN
    // ------------------------------------------------------------------

    private val enJson = """
        {
          "schemaVersion": 1,
          "levels": [
            {
              "id": 1,
              "titleId": "Level 1",
              "titleEn": "Level 1",
              "titleAr": "المستوى الأول",
              "lessons": [
                {
                  "id": "1-1",
                  "titleId": "Perkenalan",
                  "titleEn": "Introduction",
                  "titleAr": "التعارف",
                  "muhadatsah": [
                    {"speaker": "A", "ar": "السلام عليكم", "latin": "as-salāmu", "id": "Semoga keselamatan", "en": "Peace be upon you"}
                  ],
                  "mufrodat": [
                    {"ar": "اسم", "latin": "ismun", "id": "nama", "en": "name", "exampleAr": "اسمي", "exampleLatin": "ismī", "exampleId": "Namaku", "exampleEn": "My name"}
                  ],
                  "qawaid": [
                    {"titleId": "Kata tanya", "titleEn": "Question word", "id": "Penjelasan", "en": "Explanation", "exampleAr": "ما هذا", "exampleLatin": "mā hādhā", "exampleId": "Apa ini", "exampleEn": "What is this"}
                  ],
                  "tadribat": [
                    {"type": "fillBlank", "promptId": "Isilah", "promptEn": "Fill in", "promptAr": "____", "promptLatin": "____", "options": ["أ"], "answer": "أ"},
                    {"type": "translateArId", "promptAr": "ما اسمك", "promptLatin": "mā ismuk", "options": ["Siapa"], "answer": "Siapa", "optionsEn": ["Who"], "answerEn": "Who"},
                    {"type": "translateIdAr", "promptId": "Siapa", "promptEn": "Who", "options": ["من"], "answer": "من"},
                    {"type": "rearrange", "words": [{"ar": "من", "latin": "man"}], "answer": ["من"]}
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parser - field EN dibaca dan dipetakan ke model`() {
        val catalog = LughohParser.parse(enJson)
        val level = catalog.levels.first()
        assertEquals("Level 1", level.titleEn)
        val lesson = level.lessons.first()
        assertEquals("Introduction", lesson.titleEn)
        assertEquals("Peace be upon you", lesson.muhadatsah.first().en)
        val vocab = lesson.mufrodat.first()
        assertEquals("name", vocab.en)
        assertEquals("My name", vocab.exampleEn)
        val rule = lesson.qawaid.first()
        assertEquals("Question word", rule.titleEn)
        assertEquals("Explanation", rule.en)
        assertEquals("What is this", rule.exampleEn)
        val (fill, arIdEx, idArEx) = lesson.tadribat.filterIsInstance<FillBlankExercise>()
            .let { f -> Triple(f.first(), lesson.tadribat.filterIsInstance<TranslateArIdExercise>().first(), lesson.tadribat.filterIsInstance<TranslateIdArExercise>().first()) }
        assertEquals("Fill in", fill.promptEn)
        assertEquals(listOf("Who"), arIdEx.optionsEn)
        assertEquals("Who", arIdEx.answerEn)
        assertEquals("Who", idArEx.promptEn)
    }

    @Test
    fun `parser - JSON tanpa field EN memakai fallback kosong`() {
        val catalog = LughohParser.parse(enJson.replace(""" "titleEn": "Level 1",""", ""))
        assertEquals("", catalog.levels.first().titleEn)
    }

    @Test
    fun `level dan lesson - konstruktor tanpa titleEn memakai default kosong`() {
        // Memanggil konstruktor dengan argumen default (bridge $default).
        val level = LughohLevel(id = 2, titleId = "t", titleAr = "ع", lessons = emptyList())
        assertEquals("", level.titleEn)
        val lesson = LughohLesson(
            id = "1-1",
            titleId = "t",
            titleAr = "ع",
            muhadatsah = emptyList(),
            mufrodat = emptyList(),
            qawaid = emptyList(),
            tadribat = emptyList(),
        )
        assertEquals("", lesson.titleEn)
    }

    // ------------------------------------------------------------------
    // Integritas konten ASLI: setiap teks ID wajib punya terjemahan EN
    // ------------------------------------------------------------------

    private val assetsRoot: File by lazy {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File(System.getProperty("user.dir"), "src/main/assets"),
            File(System.getProperty("user.dir"), "app/src/main/assets"),
        )
        candidates.firstOrNull { it.isDirectory && File(it, "lughoh/lessons.json").isFile }
            ?: error("assets/lughoh/lessons.json tidak ditemukan — jalankan dari root repo")
    }

    private val lessonsFile: File get() = File(assetsRoot, "lughoh/lessons.json")

    private val arabicPattern = Regex("[\u0600-\u06FF\u0750-\u077F]")

    private fun requireEn(id: String, en: String, path: String) {
        assertTrue("$path: teks ID '$id' tanpa terjemahan EN", en.isNotBlank())
        assertFalse("$path: terjemahan EN '$en' memuat huruf Arab", arabicPattern.containsMatchIn(en))
    }

    @Test
    fun `integritas - semua teks Indonesia punya terjemahan Inggris`() {
        val catalog = LughohParser.parse(lessonsFile.readText())
        assertTrue("katalog tidak terbaca", catalog.levels.isNotEmpty())

        for (level in catalog.levels) {
            requireEn(level.titleId, level.titleEn, "level ${level.id}.titleEn")
            for (lesson in level.lessons) {
                val lp = "L${level.id}-${lesson.id}"
                requireEn(lesson.titleId, lesson.titleEn, "$lp.titleEn")
                lesson.muhadatsah.forEachIndexed { i, line ->
                    requireEn(line.id, line.en, "$lp.muhadatsah[$i].en")
                    // Nama orang (Ahmad, Fatimah) tidak diterjemahkan; label
                    // peran (Ustadz/Dokter/Penjual) wajib punya speakerEn.
                    if (line.speaker != "Ahmad" && line.speaker != "Salim" &&
                        line.speaker != "Fatimah" && line.speaker != "Khadijah"
                    ) {
                        assertTrue(
                            "$lp.muhadatsah[$i].speaker '${line.speaker}' tanpa speakerEn",
                            line.speakerEn.isNotBlank(),
                        )
                    }
                }
                lesson.mufrodat.forEachIndexed { i, w ->
                    requireEn(w.id, w.en, "$lp.mufrodat[$i].en")
                    requireEn(w.exampleId, w.exampleEn, "$lp.mufrodat[$i].exampleEn")
                }
                lesson.qawaid.forEachIndexed { i, g ->
                    // Judul & penjelasan kaidah sah memuat istilah Arab (mis. ضمائر).
                    assertTrue("$lp.qawaid[$i].titleEn kosong", g.titleEn.isNotBlank())
                    assertTrue("$lp.qawaid[$i].en kosong", g.en.isNotBlank())
                    requireEn(g.exampleId, g.exampleEn, "$lp.qawaid[$i].exampleEn")
                }
                lesson.tadribat.forEachIndexed { i, ex ->
                    val tp = "$lp.tadribat[$i]"
                    when (ex) {
                        is FillBlankExercise -> requireEn(ex.promptId, ex.promptEn, "$tp.promptEn")
                        is TranslateArIdExercise -> {
                            assertTrue("$tp.optionsEn kosong", ex.optionsEn.isNotEmpty())
                            requireEn("answer", ex.answerEn, "$tp.answerEn")
                            assertTrue(
                                "$tp.answerEn '${ex.answerEn}' tidak ada di optionsEn",
                                ex.answerEn in ex.optionsEn,
                            )
                            ex.optionsEn.forEach { o -> requireEn(o, o, "$tp.optionsEn") }
                        }
                        is TranslateIdArExercise -> requireEn(ex.promptId, ex.promptEn, "$tp.promptEn")
                        is RearrangeExercise -> Unit // kata Arab, tanpa terjemahan
                    }
                }
            }
        }
    }

    @Test
    fun `integritas - bahasa EN yang dipilih benar-benar tampil (end-to-end)`() {
        val catalog = LughohParser.parse(lessonsFile.readText())
        val lesson = catalog.levels.first().lessons.first()
        // Materi: baris dialog pertama tampil EN saat bahasa EN.
        assertTrue(lesson.muhadatsah.first().en.isNotBlank())
        // Latihan: translateArId EN options berisi jawaban EN yang benar.
        val arId = lesson.tadribat.filterIsInstance<TranslateArIdExercise>().first()
        val resolved = arId.forLanguage(AppLanguage.EN, Random(3))
        assertTrue(LughohEngine.isChoiceCorrect(resolved, arId.answerEn))
    }
}
