package org.opennur.tahsin.data.nahwu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NahwuParserTest {
    private val json = """
        {
          "schemaVersion": 1,
          "levels": [{
            "id": 1, "titleId": "Dasar", "titleEn": "Basics", "titleAr": "الْأَسَاسُ",
            "lessons": [{
              "id": "1-1", "titleId": "Kalimah", "titleEn": "Kalimah", "titleAr": "الْكَلِمَةُ",
              "introId": "Pengantar", "introEn": "Introduction",
              "rules": [{
                "titleId": "Isim", "titleEn": "Noun", "explanationId": "Benda",
                "explanationEn": "A thing", "exampleAr": "هٰذَا كِتَابٌ", "exampleLatin": "Hādhā kitābun",
                "exampleId": "Ini buku", "exampleEn": "This is a book"
              }],
              "exercises": [
                {"type":"choice", "promptId":"Pilih", "promptEn":"Choose", "promptAr":"هٰذَا",
                 "promptLatin":"Hādhā", "optionsId":["Isim","Fi'il","Harf"],
                 "optionsEn":["Noun","Verb","Particle"], "answerIndex":0},
                {"type":"rearrange", "promptId":"Susun", "promptEn":"Arrange",
                 "words":[{"ar":"هٰذَا","latin":"hādhā"},{"ar":"كِتَابٌ","latin":"kitābun"}]},
                {"type":"unknown", "promptId":"Lewati"}
              ]
            }]
          }]
        }
    """.trimIndent()

    @Test
    fun `parse membaca katalog dan latihan yang dikenal`() {
        val catalog = NahwuParser.parse(json)
        assertEquals(1, catalog.schemaVersion)
        assertEquals("Basics", catalog.levels[0].titleEn)
        val lesson = catalog.levels[0].lessons[0]
        assertEquals("Introduction", lesson.introEn)
        assertEquals(1, lesson.rules.size)
        assertEquals(2, lesson.exercises.size)
        val choice = lesson.exercises[0] as NahwuChoiceExercise
        assertEquals(0, choice.answerIndex)
        assertEquals("Noun", choice.optionsEn[0])
        assertEquals(2, (lesson.exercises[1] as NahwuRearrangeExercise).words.size)
    }

    @Test
    fun `parse JSON rusak atau levels null menghasilkan katalog kosong`() {
        assertTrue(NahwuParser.parse("bukan json").levels.isEmpty())
        assertTrue(NahwuParser.parse("").levels.isEmpty())
        assertTrue(NahwuParser.parse("{\"levels\":null}").levels.isEmpty())
    }

    @Test
    fun `parse latihan rusak dilewati`() {
        val broken = """
            {"levels":[{"id":1,"lessons":[{"id":"1-1","exercises":[
              {"type":"choice","optionsId":["a"],"optionsEn":["b"],"answerIndex":4},
              {"type":"rearrange","words":[{"ar":"أَنَا","latin":"anā"}]}
            ]}]}]}
        """.trimIndent()
        assertTrue(NahwuParser.parse(broken).levels[0].lessons[0].exercises.isEmpty())
    }

    @Test
    fun `parse field opsional null dan daftar null memakai default aman`() {
        val sparse = """
            {"levels":[{"id":1,"lessons":[
              {"rules":[{}],"exercises":null},
              {"id":"1-2","rules":null,"exercises":[{"type":"choice","optionsId":null,"optionsEn":null}]}
            ]},{"id":3,"lessons":null}]}
        """.trimIndent()
        val lessons = NahwuParser.parse(sparse).levels[0].lessons
        assertEquals("", lessons[0].id)
        assertEquals("", lessons[0].rules[0].titleId)
        assertTrue(lessons[0].exercises.isEmpty())
        assertTrue(lessons[1].exercises.isEmpty())
        assertTrue(NahwuParser.parse(sparse).levels[1].lessons.isEmpty())
    }
}
