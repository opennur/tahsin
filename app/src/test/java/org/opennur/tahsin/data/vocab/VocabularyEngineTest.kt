package org.opennur.tahsin.data.vocab

import org.opennur.tahsin.util.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes mesin belajar kosa kata (SRS, pemilihan sesi, kuis) — murni JVM. */
class VocabularyEngineTest {

    private val entries = listOf(
        VocabEntry("من", "مِنْ", "min", "dari", "from", 2763, example = example(2, 4, 8)),
        VocabEntry("الله", "اللّٰهِ", "allāh", "Allah", "Allah", 2156, example = example(1, 1, 2)),
        VocabEntry("ان", "اِنَّ", "inna", "sesungguhnya", "indeed", 1605, example = example(2, 6, 1)),
        VocabEntry("في", "فِيْ", "fī", "di dalam", "in", 1101, example = example(2, 10, 1)),
        VocabEntry("ما", "مَا", "mā", "apa; yang; tidak", "what; that; not", 903, example = example(2, 17, 8)),
    )
    private val now = 1_000_000L

    private fun example(surah: Int, ayah: Int, word: Int) =
        VocabExample(surah, ayah, word, "نص", "naṣṣ", "teks", "text")

    // ---- SRS ----

    @Test
    fun `SRS - kartu baru selalu jatuh tempo`() {
        val card = VocabCard("من")
        assertTrue(VocabularyEngine.isDue(card, now))
    }

    @Test
    fun `SRS - remember menaikkan kotak dan menjadwalkan ulang`() {
        val card = VocabCard("من", box = 1, nextDue = now)
        val after = VocabularyEngine.remember(card, now)
        assertEquals(2, after.box)
        assertEquals(now + 3 * 86_400_000L, after.nextDue) // box 2 = 3 hari
        assertEquals(1, after.correctCount)
        assertFalse(VocabularyEngine.isDue(after, now))
        // Kotak maksimum 5 (interval 30 hari).
        val maxed = VocabularyEngine.remember(VocabCard("من", box = 5), now)
        assertEquals(5, maxed.box)
        assertEquals(now + 30 * 86_400_000L, maxed.nextDue)
    }

    @Test
    fun `SRS - forget menurunkan ke kotak 0 dan ulang hari ini`() {
        val card = VocabCard("من", box = 4, nextDue = now + 1000)
        val after = VocabularyEngine.forget(card, now)
        assertEquals(0, after.box)
        assertEquals(now, after.nextDue)
        assertEquals(1, after.wrongCount)
        assertTrue(VocabularyEngine.isDue(after, now))
    }

    @Test
    fun `dayKey - format tanggal stabil`() {
        val key = VocabularyEngine.dayKey(now)
        assertTrue(Regex("""\d{4}-\d{2}-\d{2}""").matches(key))
    }

    // ---- Sesi ----

    @Test
    fun `selectSession - kartu jatuh tempo dulu, lalu kata baru`() {
        val cards = mapOf(
            "ان" to VocabCard("ان", box = 2, nextDue = now - 1),   // due (tertua)
            "الله" to VocabCard("الله", box = 3, nextDue = now + 1000), // belum due
        )
        val session = VocabularyEngine.selectSession(entries, cards, now, newLimit = 2, dueLimit = 10)
        assertEquals(listOf("ان", "من", "في"), session.map { it.key })
    }

    @Test
    fun `selectSession - batas due & baru dihormati`() {
        val cards = mapOf(
            "ان" to VocabCard("ان", nextDue = now - 5),
            "ما" to VocabCard("ما", nextDue = now - 4),
        )
        val session = VocabularyEngine.selectSession(entries, cards, now, newLimit = 1, dueLimit = 1)
        // due paling tua ("ان") + 1 kata baru terfrequent ("من").
        assertEquals(listOf("ان", "من"), session.map { it.key })
    }

    @Test
    fun `selectSession - kata baru beragam akar (satu anggota per akar)`() {
        val rooted = listOf(
            VocabEntry("قال", "قَالَ", "qāla", "dia berkata", "he said", 411, root = "قول"),
            VocabEntry("قالوا", "قَالُوْا", "qālū", "mereka berkata", "they said", 250, root = "قول"),
            VocabEntry("كتب", "كِتَابٌ", "kitāb", "kitab", "book", 66, root = "كتب"),
            VocabEntry("خلق", "خَلَقَ", "khalaqa", "Dia menciptakan", "He created", 83, root = "خلق"),
            VocabEntry("علم", "عِلْمَ", "'ilm", "ilmu", "knowledge", 70, root = "علم"),
        )
        val session = VocabularyEngine.selectSession(rooted, emptyMap(), now, newLimit = 4, dueLimit = 0)
        val keys = session.map { it.key }
        // قال & قالوا satu akar (قول) → hanya قال yang masuk; akar lain ikut.
        assertEquals(listOf("قال", "كتب", "خلق", "علم"), keys)
        assertEquals(4, session.map { it.root }.distinct().size)
    }

    // ---- Kuis ----

    @Test
    fun `makeQuiz - 4 opsi arti, index benar konsisten, arti pengecoh unik`() {
        val q = VocabularyEngine.makeQuiz(entries, entries[0], AppLanguage.ID, random = Random(7))!!
        assertEquals(4, q.options.size)
        assertEquals("من", q.answerKey)
        assertEquals("مِنْ", q.prompt)
        assertEquals("min", q.promptTranslit)
        assertEquals("dari", q.options[q.correctIndex])
        assertTrue(q.options.contains("dari"))
        // Tidak ada pengecoh dengan arti sama dengan target.
        assertFalse(q.options.any { it == "dari" && q.options.indexOf(it) != q.correctIndex })
        assertNotNull(q.example)
    }

    @Test
    fun `makeQuiz - mode balik menanyakan arti, opsi berupa kata`() {
        val q = VocabularyEngine.makeQuiz(entries, entries[0], AppLanguage.EN, reverse = true, random = Random(3))!!
        assertEquals("from", q.prompt)
        assertEquals("", q.promptTranslit)
        assertEquals("مِنْ", q.options[q.correctIndex])
        assertEquals(4, q.options.size)
        assertTrue(q.options.all { it.isNotBlank() })
    }

    @Test
    fun `makeQuiz - kolam pengecoh kurang dari 3 - null`() {
        val tiny = entries.take(2)
        assertNull(VocabularyEngine.makeQuiz(tiny, tiny[0], AppLanguage.ID, random = Random(1)))
    }

    @Test
    fun `meaningOf - pilih bahasa aktif`() {
        assertEquals("dari", VocabularyEngine.meaningOf(entries[0], AppLanguage.ID))
        assertEquals("from", VocabularyEngine.meaningOf(entries[0], AppLanguage.EN))
    }


    // ---- gap coverage ----

    @Test
    fun `intervalDays - semua kotak SRS`() {
        assertEquals(0L, VocabularyEngine.intervalDays(0))
        assertEquals(1L, VocabularyEngine.intervalDays(1))
        assertEquals(3L, VocabularyEngine.intervalDays(2))
        assertEquals(7L, VocabularyEngine.intervalDays(3))
        assertEquals(14L, VocabularyEngine.intervalDays(4))
        assertEquals(30L, VocabularyEngine.intervalDays(5))
        assertEquals(30L, VocabularyEngine.intervalDays(9)) // else
    }

    @Test
    fun `selectSession - dengan default argumen newLimit dan dueLimit - tetap jalan`() {
        val session = VocabularyEngine.selectSession(entries, mapOf(), now)
        assertTrue(session.isNotEmpty())
        assertTrue(session.size <= 5)
    }

    @Test
    fun `selectSession - satu anggota per akar pada kata baru`() {
        val sameRoot = listOf(
            VocabEntry("ك1", "ك1", "k1", "m1", "e1", 10, root = "akar"),
            VocabEntry("ك2", "ك2", "k2", "m2", "e2", 9, root = "akar"),
            VocabEntry("lain", "lain", "l", "m3", "e3", 8, root = "akar-lain"),
        )
        val session = VocabularyEngine.selectSession(sameRoot, mapOf(), now, newLimit = 5)
        val roots = session.map { it.root }
        assertEquals(2, session.size)
        assertEquals(2, roots.toSet().size) // "akar" hanya muncul sekali
    }

    @Test
    fun `selectSession - akar yang sudah jatuh tempo tidak dipilih sebagai kata baru`() {
        val due = VocabEntry("due", "due", "d", "m", "e", 100, root = "akarX")
        val fresh = VocabEntry("fresh", "fresh", "f", "m2", "e2", 50, root = "akarX")
        val session = VocabularyEngine.selectSession(
            listOf(due, fresh),
            mapOf("due" to VocabCard("due", nextDue = 0L)),
            now,
            newLimit = 5,
        )
        assertTrue(session.any { it.key == "due" })
        assertFalse(session.any { it.key == "fresh" }) // akar sama dengan due
    }

    @Test
    fun `makeQuiz - dengan default argumen (reverse=false, random) - jalan`() {
        val q = VocabularyEngine.makeQuiz(entries, entries[0], AppLanguage.ID)
        assertNotNull(q)
        assertEquals(4, q!!.options.size)
        assertEquals(entries[0].word, q.prompt) // mode depan: kata Arab
        assertEquals(entries[0].translit, q.promptTranslit)
    }

    @Test
    fun `model - getter yang jarang dibaca tetap konsisten`() {
        val card = VocabCard(key = "من", box = 2, nextDue = 5L, correctCount = 3, wrongCount = 1)
        assertEquals("من", card.key)
        assertEquals(2, card.box)
        assertEquals(5L, card.nextDue)
        assertEquals(3, card.correctCount)
        assertEquals(1, card.wrongCount)

        val daily = VocabDaily(date = "2026-08-13", studied = 4, learned = 2)
        assertEquals("2026-08-13", daily.date)
        assertEquals(4, daily.studied)
        assertEquals(2, daily.learned)

        val ex = example(2, 4, 8)
        assertEquals("نص", ex.ayahArab)
        assertEquals("naṣṣ", ex.ayahLatin)
        assertEquals("teks", ex.ayahId)
        assertEquals("text", ex.ayahEn)
        assertEquals(8, ex.word)
    }


    @Test
    fun `selectSession - pengurutan due (nextDue lalu frekuensi)`() {
        val extra = listOf(
            VocabEntry("a", "a", "a", "m", "e", 50),
            VocabEntry("b", "b", "b", "m", "e", 100),
            VocabEntry("c", "c", "c", "m", "e", 30),
        )
        val cards = mapOf(
            "c" to VocabCard("c", nextDue = 100L), // nextDue sama dengan b → tiebreak frekuensi
            "b" to VocabCard("b", nextDue = 100L),
            "a" to VocabCard("a", nextDue = 50L),
        )
        val session = VocabularyEngine.selectSession(entries + extra, cards, now, dueLimit = 10)
        assertEquals(listOf("a", "b", "c"), session.take(3).map { it.key })
    }


    @Test
    fun `makeQuiz - entri makna kosong dan makna duplikat dibuang dari kolam`() {
        val blank = VocabEntry("kosong", "kosong", "k", "", "", 5)
        val dup = VocabEntry("dupe", "dupe", "d", "dari", "e1", 5) // makna sama dengan entries[0]
        val quiz = VocabularyEngine.makeQuiz(entries + blank + dup, entries[0], AppLanguage.ID)
        assertNotNull(quiz)
        assertFalse(quiz!!.options.contains(""))
        assertEquals(4, quiz.options.size)
    }
}
