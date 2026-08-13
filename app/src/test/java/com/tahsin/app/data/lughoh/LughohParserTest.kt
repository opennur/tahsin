package com.tahsin.app.data.lughoh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes parsing `assets/lughoh/lessons.json` (murni JVM). */
class LughohParserTest {

    private val sample = """
        {"schemaVersion":1,"levels":[
          {"id":1,"titleId":"Level 1 — Tes","titleAr":"المُسْتَوَى الأَوَّل","lessons":[
            {"id":"1-1","titleId":"Perkenalan","titleAr":"التَّعْرِيفُ",
             "muhadatsah":[
               {"speaker":"Ahmad","ar":"مَا اسْمُكِ؟","latin":"Mā ismuki?","id":"Siapa namamu?"}
             ],
             "mufrodat":[
               {"ar":"اِسْم","latin":"ism","id":"nama","exampleAr":"مَا اسْمُكِ؟","exampleLatin":"Mā ismuki?","exampleId":"Siapa namamu?"}
             ],
             "qawaid":[
               {"titleId":"Kata tanya","id":"Penjelasan","exampleAr":"مَا اسْمُكِ؟","exampleLatin":"Mā ismuki?","exampleId":"Siapa namamu?"}
             ],
             "tadribat":[
               {"type":"fillBlank","promptId":"Isi","promptAr":"مَا ____؟","promptLatin":"Mā ____?",
                "options":["اسْمُكِ","بَيْتُكِ","كِتَابُكِ","قَلَمُكِ"],"answer":"اسْمُكِ"},
               {"type":"translateArId","promptAr":"مَا اسْمُكِ؟","promptLatin":"Mā ismuki?",
                "options":["Siapa namamu?","Apa kabarmu?","Dari mana kamu?","Di mana rumahmu?"],"answer":"Siapa namamu?"},
               {"type":"translateIdAr","promptId":"Senang berkenalan.",
                "options":["تَشَرَّفْنَا","أَهْلًا وَسَهْلًا","مَعَ السَّلَامَةِ","صَبَاحَ الْخَيْرِ"],"answer":"تَشَرَّفْنَا"},
               {"type":"rearrange",
                "words":[{"ar":"أَنَا","latin":"anā"},{"ar":"مِنْ","latin":"min"},{"ar":"إِنْدُونِيسِيَّا","latin":"Indūnīsiyyā"}],
                "answer":["أَنَا","مِنْ","إِنْدُونِيسِيَّا"]},
               {"type":"hujian","promptAr":"x","options":["a"],"answer":"a"}
             ]}
          ]}
        ]}
    """.trimIndent()

    @Test
    fun `parse - katalog, level, dan lesson terbaca`() {
        val catalog = LughohParser.parse(sample)
        assertEquals(1, catalog.schemaVersion)
        assertEquals(1, catalog.levels.size)
        val level = catalog.levels[0]
        assertEquals(1, level.id)
        assertEquals("Level 1 — Tes", level.titleId)
        assertEquals("المُسْتَوَى الأَوَّل", level.titleAr)
        assertEquals(1, level.lessons.size)
        val lesson = level.lessons[0]
        assertEquals("1-1", lesson.id)
        assertEquals("Perkenalan", lesson.titleId)
    }

    @Test
    fun `parse - muhadatsah, mufrodat, qawaid terbaca`() {
        val lesson = LughohParser.parse(sample).levels[0].lessons[0]
        val line = lesson.muhadatsah[0]
        assertEquals("Ahmad", line.speaker)
        assertEquals("مَا اسْمُكِ؟", line.ar)
        assertEquals("Mā ismuki?", line.latin)
        assertEquals("Siapa namamu?", line.id)

        val word = lesson.mufrodat[0]
        assertEquals("اِسْم", word.ar)
        assertEquals("ism", word.latin)
        assertEquals("nama", word.id)
        assertEquals("مَا اسْمُكِ؟", word.exampleAr)

        val rule = lesson.qawaid[0]
        assertEquals("Kata tanya", rule.titleId)
        assertEquals("Penjelasan", rule.id)
        assertEquals("مَا اسْمُكِ؟", rule.exampleAr)
    }

    @Test
    fun `parse - keempat jenis tadribat dipetakan, jenis tak dikenal di-skip`() {
        val tadribat = LughohParser.parse(sample).levels[0].lessons[0].tadribat
        assertEquals(4, tadribat.size)

        val fill = tadribat[0] as FillBlankExercise
        assertEquals(ExerciseType.FILL_BLANK, fill.type)
        assertEquals("اسْمُكِ", fill.answer)
        assertEquals(listOf("اسْمُكِ", "بَيْتُكِ", "كِتَابُكِ", "قَلَمُكِ"), fill.options)
        assertEquals("مَا ⋯⋯؟", fill.displayPromptAr)

        val arId = tadribat[1] as TranslateArIdExercise
        assertEquals(ExerciseType.TRANSLATE_AR_ID, arId.type)
        assertEquals("Siapa namamu?", arId.answer)

        val idAr = tadribat[2] as TranslateIdArExercise
        assertEquals(ExerciseType.TRANSLATE_ID_AR, idAr.type)
        assertEquals("Senang berkenalan.", idAr.promptId)

        val re = tadribat[3] as RearrangeExercise
        assertEquals(ExerciseType.REARRANGE, re.type)
        assertEquals(3, re.words.size)
        assertEquals("أَنَا", re.words[0].ar)
        assertEquals(listOf("أَنَا", "مِنْ", "إِنْدُونِيسِيَّا"), re.answer)
    }

    @Test
    fun `parse - JSON rusak atau kosong - katalog kosong (tidak crash)`() {
        assertTrue(LughohParser.parse("bukan json").levels.isEmpty())
        assertTrue(LughohParser.parse("").levels.isEmpty())
        assertTrue(LughohParser.parse("""{"levels":null}""").levels.isEmpty())
    }

    @Test
    fun `parse - latihan dengan tipe answer salah di-skip, bukan crash`() {
        // fillBlank tetapi answer berupa array → latihan itu dilewati,
        // latihan lain tetap terbaca (parser tidak boleh crash).
        val json = """
            {"levels":[{"id":1,"lessons":[
              {"id":"1-1","muhadatsah":[{"speaker":"A","ar":"مَا اسْمُكِ؟","latin":"Mā ismuki?","id":"Siapa namamu?"}],
               "mufrodat":[],"qawaid":[],
               "tadribat":[
                 {"type":"fillBlank","promptAr":"مَا ____؟","promptLatin":"Mā ____?",
                  "options":["اسْمُكِ","بَيْتُكِ","كِتَابُكِ","قَلَمُكِ"],"answer":["اسْمُكِ"]},
                 {"type":"translateArId","promptAr":"مَا اسْمُكِ؟","promptLatin":"Mā ismuki?",
                  "options":["Siapa namamu?","Apa kabarmu?","Dari mana kamu?","Di mana rumahmu?"],"answer":"Siapa namamu?"}
               ]}
            ]}]}
        """.trimIndent()
        val tadribat = LughohParser.parse(json).levels[0].lessons[0].tadribat
        assertEquals(1, tadribat.size)
        assertTrue(tadribat[0] is TranslateArIdExercise)
    }
}
