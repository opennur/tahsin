package org.opennur.tahsin.data.nahwu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NahwuEngineTest {
    private val choice = NahwuChoiceExercise(
        promptId = "Pilih",
        promptEn = "Choose",
        promptAr = "هٰذَا",
        promptLatin = "Hādhā",
        optionsId = listOf("Isim", "Fi'il", "Harf"),
        optionsEn = listOf("Noun", "Verb", "Particle"),
        answerIndex = 0,
    )
    private val rearrange = NahwuRearrangeExercise(
        promptId = "Susun",
        promptEn = "Arrange",
        words = listOf(NahwuWord("أَنَا", "anā"), NahwuWord("طَالِبٌ", "ṭālibun")),
    )

    private fun lesson(id: String, vararg exercises: NahwuExercise) = NahwuLesson(
        id = id,
        titleId = id,
        titleEn = id,
        titleAr = id,
        introId = id,
        introEn = id,
        rules = emptyList(),
        exercises = exercises.toList(),
    )

    @Test
    fun `pilihan benar dan salah`() {
        assertTrue(NahwuEngine.isChoiceCorrect(choice, 0))
        assertFalse(NahwuEngine.isChoiceCorrect(choice, 2))
    }

    @Test
    fun `pilihan jawaban batas indeks pertama dan terakhir`() {
        val first = NahwuChoiceExercise("a", "a", "a", "a", listOf("X", "Y", "Z"), listOf("X", "Y", "Z"), 0)
        val last = NahwuChoiceExercise("b", "b", "b", "b", listOf("X", "Y", "Z"), listOf("X", "Y", "Z"), 2)
        assertTrue(NahwuEngine.isChoiceCorrect(first, 0))
        assertTrue(NahwuEngine.isChoiceCorrect(last, 2))
        assertFalse(NahwuEngine.isChoiceCorrect(first, 1))
        assertFalse(NahwuEngine.isChoiceCorrect(last, 1))
    }

    @Test
    fun `sesi acak mengambil dari semua lesson`() {
        val lessons = (1..5).map { lesson("$it", choice, rearrange) }
        val session = NahwuEngine.buildSession(lessons, 8, Random(42))
        assertEquals(8, session.size)
        assertTrue(session.all { it.lessonId in lessons.map(NahwuLesson::id) })
        assertEquals(0, NahwuEngine.buildSession(emptyList(), 8).size)
        assertEquals(2, NahwuEngine.buildSession(listOf(lesson("one", choice, rearrange)), 99).size)
        val one = lesson("one", choice, rearrange)
        assertEquals(listOf("one:0", "one:1"), NahwuEngine.allQuestionIds(listOf(one)))
        assertEquals(1, NahwuEngine.buildSession(listOf(one), 8, Random(1), setOf("one:1")).size)
    }

    @Test
    fun `sesi lebih banyak pertanyaan daripada latihan`() {
        val lessons = listOf(lesson("1", choice))
        val session = NahwuEngine.buildSession(lessons, 100, Random(42))
        assertEquals(1, session.size)
    }

    @Test
    fun `sesi dengan banyak latihan per lesson`() {
        val exercises = (1..20).map { i ->
            NahwuChoiceExercise("$i", "$i", "$i", "$i", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val lessons = listOf(lesson("1", *exercises.toTypedArray()))
        val session = NahwuEngine.buildSession(lessons, 8, Random(42))
        assertEquals(8, session.size)
    }

    @Test
    fun `allowedIds memfilter sesi`() {
        val exercises = (1..5).map { i ->
            NahwuChoiceExercise("$i", "$i", "$i", "$i", listOf("A", "B", "C"), listOf("A", "B", "C"), 0)
        }
        val lessons = listOf(lesson("1", *exercises.toTypedArray()))
        val allIds = NahwuEngine.allQuestionIds(lessons)
        assertEquals(5, allIds.size)
        val filtered = NahwuEngine.buildSession(lessons, 10, Random(42), setOf("1:2", "1:4"))
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.questionId in setOf("1:2", "1:4") })
    }

    @Test
    fun `allowedIds kosong menghasilkan sesi kosong`() {
        val lessons = listOf(lesson("1", choice))
        val session = NahwuEngine.buildSession(lessons, 8, Random(42), emptySet())
        assertEquals(0, session.size)
    }

    @Test
    fun `allQuestionIds dengan lesson kosong`() {
        assertEquals(emptyList<String>(), NahwuEngine.allQuestionIds(emptyList()))
    }

    @Test
    fun `allQuestionIds urut berdasarkan lesson dan indeks`() {
        val l1 = lesson("1-1", choice, rearrange, choice)
        val l2 = lesson("1-2", rearrange)
        val ids = NahwuEngine.allQuestionIds(listOf(l1, l2))
        assertEquals(listOf("1-1:0", "1-1:1", "1-1:2", "1-2:0"), ids)
    }

    @Test
    fun `questionId format benar`() {
        assertEquals("abc:5", NahwuEngine.questionId("abc", 5))
        assertEquals("1-3:0", NahwuEngine.questionId("1-3", 0))
    }

    @Test
    fun `acak kata mempertahankan isi dan mengubah urutan`() {
        val shown = NahwuEngine.shuffleWords(rearrange, Random(42))
        assertEquals(rearrange.words.toSet(), shown.toSet())
        assertNotEquals(rearrange.words, shown)
        assertEquals(rearrange.words.size, NahwuEngine.shuffleWords(rearrange).size)
        repeat(100) { NahwuEngine.shuffleWords(rearrange, Random(it)) }
        val duplicateWords = rearrange.copy(words = listOf(rearrange.words[0], rearrange.words[0]))
        assertEquals(duplicateWords.words, NahwuEngine.shuffleWords(duplicateWords, Random(1)))
        val single = rearrange.copy(words = listOf(rearrange.words.first()))
        assertEquals(single.words, NahwuEngine.shuffleWords(single, Random(1)))
    }

    @Test
    fun `acak kata dengan banyak kata mempertahankan semua`() {
        val words = listOf(
            NahwuWord("ذَهَبَ", "dhahaba"),
            NahwuWord("الطَّالِبُ", "aṭ-ṭālibu"),
            NahwuWord("إِلَى", "ilā"),
            NahwuWord("الْمَدْرَسَةِ", "al-madrasati"),
            NahwuWord("فِي", "fī"),
            NahwuWord("الْبَيْتِ", "al-bayti"),
        )
        val exercise = NahwuRearrangeExercise("a", "a", words)
        val shuffled = NahwuEngine.shuffleWords(exercise, Random(42))
        assertEquals(words.size, shuffled.size)
        assertEquals(words.toSet(), shuffled.toSet())
    }

    @Test
    fun `acak kata dengan dua kata selalu mengubah urutan`() {
        val words = listOf(NahwuWord("أَ", "a-"), NahwuWord("لَ", "la-"))
        val exercise = NahwuRearrangeExercise("a", "a", words)
        var changed = false
        repeat(50) { i ->
            val s = NahwuEngine.shuffleWords(exercise, Random(i.toLong()))
            if (s != words) changed = true
        }
        assertTrue(changed)
    }

    @Test
    fun `susun memvalidasi urutan dan panjang`() {
        val shown = listOf(rearrange.words[1], rearrange.words[0])
        assertTrue(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(1, 0)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(0, 1)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, shown, listOf(1)))
        val noMarks = rearrange.words.map { it.copy(ar = it.ar.replace("َ", "")) }
        assertTrue(NahwuEngine.isRearrangeCorrect(rearrange, noMarks, listOf(0, 1)))
        assertFalse(NahwuEngine.isRearrangeCorrect(rearrange, emptyList(), listOf(0, 1)))
    }

    @Test
    fun `susun normalisasi harakat saat membandingkan`() {
        val ex = NahwuRearrangeExercise("a", "a", listOf(
            NahwuWord("كِتَابٌ", "kitābun"),
            NahwuWord("كَبِيرٌ", "kabīrun"),
        ))
        val withHarakat = listOf(NahwuWord("كَبِيرٌ", "kabīrun"), NahwuWord("كِتَابٌ", "kitābun"))
        val withoutHarakat = listOf(NahwuWord("كبير", "kabīrun"), NahwuWord("كتاب", "kitābun"))
        assertTrue(NahwuEngine.isRearrangeCorrect(ex, withHarakat, listOf(1, 0)))
        assertTrue(NahwuEngine.isRearrangeCorrect(ex, withoutHarakat, listOf(1, 0)))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, withHarakat, listOf(0, 1)))
    }

    @Test
    fun `susun dengan kata identik tapi indeks berbeda`() {
        val ex = NahwuRearrangeExercise("a", "a", listOf(
            NahwuWord("مِنْ", "min"),
            NahwuWord("فِي", "fī"),
        ))
        val shown = listOf(NahwuWord("فِي", "fī"), NahwuWord("مِنْ", "min"))
        assertTrue(NahwuEngine.isRearrangeCorrect(ex, shown, listOf(1, 0)))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, shown, listOf(0, 1)))
    }

    @Test
    fun `susun panjang tidak cocok mengembalikan salah`() {
        val ex = NahwuRearrangeExercise("a", "a", listOf(
            NahwuWord("أَ", "a-"),
            NahwuWord("لَ", "la-"),
            NahwuWord("بَ", "ba-"),
        ))
        val shown = listOf(NahwuWord("لَ", "la-"), NahwuWord("بَ", "ba-"))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, shown, listOf(1, 0)))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, shown, listOf(0, 1)))
    }

    @Test
    fun `susun indeks di luar batas shown`() {
        val ex = NahwuRearrangeExercise("a", "a", listOf(
            NahwuWord("أَ", "a-"),
            NahwuWord("بَ", "ba-"),
        ))
        val shown = listOf(NahwuWord("أَ", "a-"))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, shown, listOf(0, 1)))
    }

    @Test
    fun `susun dengan banyak kata lebih dari lima`() {
        val words = (1..8).map { NahwuWord("كَلِمَة$it", "kalima$it") }
        val ex = NahwuRearrangeExercise("a", "a", words)
        val shuffled = words.reversed()
        assertTrue(NahwuEngine.isRearrangeCorrect(ex, shuffled, (7 downTo 0).toList()))
        assertFalse(NahwuEngine.isRearrangeCorrect(ex, shuffled, (0..7).toList()))
    }

    @Test
    fun `sesi deterministik dengan random seed sama`() {
        val lessons = (1..3).map { lesson("$it", choice, rearrange) }
        val s1 = NahwuEngine.buildSession(lessons, 6, Random(99))
        val s2 = NahwuEngine.buildSession(lessons, 6, Random(99))
        assertEquals(s1.map { it.questionId }, s2.map { it.questionId })
    }

    @Test
    fun `sesi count nol menghasilkan kosong`() {
        val lessons = listOf(lesson("1", choice))
        assertTrue(NahwuEngine.buildSession(lessons, 0).isEmpty())
    }

    @Test
    fun `pilihan dengan banyak opsi`() {
        val ex = NahwuChoiceExercise(
            "a", "a", "a", "a",
            listOf("Opsi1", "Opsi2", "Opsi3", "Opsi4", "Opsi5"),
            listOf("Opt1", "Opt2", "Opt3", "Opt4", "Opt5"),
            3,
        )
        assertFalse(NahwuEngine.isChoiceCorrect(ex, 0))
        assertFalse(NahwuEngine.isChoiceCorrect(ex, 2))
        assertTrue(NahwuEngine.isChoiceCorrect(ex, 3))
        assertFalse(NahwuEngine.isChoiceCorrect(ex, 4))
    }

    @Test
    fun `exerciseQuestionId unik untuk banyak exercise`() {
        val exercises = (0..9).map { i ->
            NahwuChoiceExercise("$i", "$i", "$i", "$i", listOf("A"), listOf("A"), 0)
        }
        val l = lesson("1-1", *exercises.toTypedArray())
        val ids = NahwuEngine.allQuestionIds(listOf(l))
        assertEquals(ids.size, ids.toSet().size)
    }
}
