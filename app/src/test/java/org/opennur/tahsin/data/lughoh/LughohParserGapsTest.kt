package org.opennur.tahsin.data.lughoh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Gap-coverage parser & pengacakan latihan Belajar Arab. */
class LughohParserGapsTest {

    @Test
    fun `parse - latihan dengan answer bertipe angka - dilewati (bukan crash)`() {
        val json = """{
            "schemaVersion": 1,
            "levels": [{
                "id": 1,
                "titleId": "Level 1",
                "lessons": [{
                    "id": "l1",
                    "titleId": "Pelajaran",
                    "titleAr": "الدرس",
                    "tadribat": [
                        {"type": "fillBlank", "promptAr": "أنا ____ طالب", "options": ["طالب", "خاطئ"], "answer": {"rusak": true}},
                        {"type": "translateIdAr", "promptId": "Saya pelajar", "options": ["أنا طالب"], "answer": "أنا طالب"}
                    ]
                }]
            }]
        }"""
        val catalog = LughohParser.parse(json)
        val tadribat = catalog.levels.single().lessons.single().tadribat
        assertEquals(1, tadribat.size) // latihan rusak di-skip
        assertEquals(ExerciseType.TRANSLATE_ID_AR, tadribat[0].type)
    }

    @Test
    fun `parse - levels dan schemaVersion null - default aman`() {
        val catalog = LughohParser.parse("{}")
        assertEquals(0, catalog.schemaVersion)
        assertTrue(catalog.levels.isEmpty())
    }

    @Test
    fun `shuffleOptions - rearrangeExercise - objek sama (tidak diacak)`() {
        val ex = RearrangeExercise(
            words = listOf(WordChip("أنا", "ana"), WordChip("طالب", "thālib")),
            answer = listOf("أنا", "طالب"),
        )
        assertEquals(ex, LughohEngine.shuffleOptions(ex, Random(1)))
    }

    @Test
    fun `shuffleRearrange - satu kata - urutan tetap`() {
        val chips = listOf(WordChip("أنا", "ana"))
        val ex = RearrangeExercise(words = chips, answer = listOf("أنا"))
        assertEquals(chips, LughohEngine.shuffleRearrange(ex, Random(1)))
    }

    @Test
    fun `shuffleRearrange - urutan tidak pernah sama dengan aslinya`() {
        val chips = listOf(WordChip("أ", "a"), WordChip("ب", "b"), WordChip("ج", "c"))
        val ex = RearrangeExercise(words = chips, answer = listOf("أ", "ب", "ج"))
        val order = LughohEngine.shuffleRearrange(ex, Random(42))
        assertEquals(3, order.size)
        // Guard-loop menjamin hasil beda dari urutan asli.
        assertTrue(order != chips)
    }


    @Test
    fun `parse - fillBlank tanpa field answer - dilewati`() {
        val json = """{
            "levels": [{
                "id": 1,
                "lessons": [{
                    "id": "l1",
                    "tadribat": [
                        {"type": "fillBlank", "promptAr": "أ ____ ب", "options": ["أ", "ب"]}
                    ]
                }]
            }]
        }"""
        val catalog = LughohParser.parse(json)
        assertTrue(catalog.levels.single().lessons.single().tadribat.isEmpty())
    }
}
