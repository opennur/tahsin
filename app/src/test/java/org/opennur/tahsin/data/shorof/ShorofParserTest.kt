@file:Suppress("MaxLineLength")

package org.opennur.tahsin.data.shorof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShorofParserTest {
    private val json = """
        {"schemaVersion":1,"levels":[{"id":1,"titleId":"Dasar","titleEn":"Basics","titleAr":"الْأَسَاسُ","lessons":[
          {"id":"1-1","titleId":"Akar","titleEn":"Roots","titleAr":"الْجِذْرُ","introId":"Pengantar","introEn":"Intro",
           "rules":[{"titleId":"Akar","titleEn":"Root","explanationId":"Tiga huruf","explanationEn":"Three letters","exampleAr":"كَتَبَ","exampleLatin":"kataba","exampleId":"menulis","exampleEn":"write"}],
           "patterns":[{"root":"ك ت ب","rootLatin":"k-t-b","wazan":"فَعَلَ","wazanLatin":"faʿala","formId":"Dasar","formEn":"Basic","meaningId":"menulis","meaningEn":"write","exampleAr":"كَتَبَ","exampleLatin":"kataba"}],
           "conjugations":[{"pronounAr":"هُوَ","pronounLatin":"huwa","past":"كَتَبَ","present":"يَكْتُبُ","imperative":"اُكْتُبْ"}],
           "exercises":[{"type":"choice","promptId":"Pilih","promptEn":"Choose","promptAr":"كَتَبَ","promptLatin":"kataba","optionsId":["A","B","C"],"optionsEn":["A","B","C"],"answerIndex":0},{"type":"bad"}]}
        ]}]}
    """.trimIndent()

    @Test
    fun `parse membaca lesson pattern tasrif dan soal`() {
        val lesson = ShorofParser.parse(json).levels[0].lessons[0]
        assertEquals("Roots", lesson.titleEn)
        assertEquals("Three letters", lesson.rules[0].explanationEn)
        assertEquals("فَعَلَ", lesson.patterns[0].wazan)
        assertEquals("كَتَبَ", lesson.conjugations[0].past)
        assertEquals(1, lesson.exercises.size)
        assertEquals(0, (lesson.exercises[0] as ShorofChoiceExercise).answerIndex)
    }

    @Test
    fun `parse JSON rusak atau field null aman`() {
        assertTrue(ShorofParser.parse("bukan json").levels.isEmpty())
        assertTrue(ShorofParser.parse("{\"levels\":null}").levels.isEmpty())
        val sparse = ShorofParser.parse("""
            {"levels":[
              {"lessons":[{"rules":[{}],"patterns":[{}],"conjugations":[{}],"exercises":[
                {"type":"choice","optionsId":["a","b","c"],"optionsEn":["a","b","c"],"answerIndex":0},
                {"type":"choice","optionsId":["a"],"optionsEn":["a"],"answerIndex":0},
                {"type":"choice","optionsId":["a","b","c"],"optionsEn":["a","b"],"answerIndex":0},
                {"type":"choice","optionsId":["a","b","c"],"optionsEn":["a","b","c"]},{}
              ]}]},
              {"lessons":null},
              {"lessons":[{"rules":null,"patterns":null,"conjugations":null,"exercises":null}]}
            ]}
        """.trimIndent())
        assertEquals("", sparse.levels[0].lessons[0].rules[0].titleId)
        assertEquals("", sparse.levels[0].lessons[0].patterns[0].root)
        assertEquals("", sparse.levels[0].lessons[0].conjugations[0].past)
        assertEquals(1, sparse.levels[0].lessons[0].exercises.size)
        assertTrue(sparse.levels[1].lessons.isEmpty())
        assertTrue(sparse.levels[2].lessons[0].rules.isEmpty())
    }
}
