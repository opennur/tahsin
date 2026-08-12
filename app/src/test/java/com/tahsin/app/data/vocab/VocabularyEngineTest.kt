package com.tahsin.app.data.vocab

import com.tahsin.app.util.AppLanguage
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
}
