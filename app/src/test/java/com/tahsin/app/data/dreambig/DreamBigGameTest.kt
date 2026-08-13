package com.tahsin.app.data.dreambig

import com.tahsin.app.data.vocab.VocabEntry
import com.tahsin.app.util.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes aturan main game Dream BIG (murni JVM). */
class DreamBigGameTest {

    private fun entry(key: String, meaning: String, freq: Int = 1) = VocabEntry(
        key = key, word = key, translit = "t", meaningId = meaning, meaningEn = "en:$meaning",
        freq = freq,
    )

    private fun level(vararg keys: String) = DreamBigLevel(day = 1, title = "Day 1", wordKeys = keys.toList())

    // ---- wordsFor ----

    @Test
    fun `wordsFor - kunci terpetakan, hilang atau arti kosong dilewati`() {
        val entries = listOf(
            entry("من", "dari"),
            entry("قال", "berkata"),
            entry("skeleton", ""), // arti kosong → dilewati
        )
        val words = DreamBigGame.wordsFor(level("من", "قال", "missing", "skeleton"), entries)
        assertEquals(listOf("من", "قال"), words.map { it.key })
    }

    // ---- pickTargets ----

    @Test
    fun `pickTargets - unik, sebanyak count, deterministik per random`() {
        val pool = (1..30).map { entry("k$it", "arti$it") }
        val r1 = Random(42)
        val r2 = Random(42)
        val a = DreamBigGame.pickTargets(pool, 10, r1)
        val b = DreamBigGame.pickTargets(pool, 10, r2)
        assertEquals(a, b) // Random dengan seed sama → hasil sama
        assertEquals(10, a.size)
        assertEquals(10, a.distinct().size)
        assertTrue(a.all { it in pool })
    }

    // ---- question ----

    @Test
    fun `question - pilihan ganda valid (4 opsi, 1 benar)`() {
        val pool = (1..20).map { entry("k$it", "arti$it") }
        val q = DreamBigGame.question(pool, pool[0], AppLanguage.ID, reverse = false, random = Random(1))
        assertNotNull(q)
        assertEquals(4, q!!.options.size)
        assertEquals(q.options[q.correctIndex], "arti1")
        assertTrue(q.promptTranslit.isNotEmpty())
    }

    @Test
    fun `question - reverse menampilkan kata sebagai opsi`() {
        val pool = (1..20).map { entry("k$it", "arti$it") }
        val q = DreamBigGame.question(pool, pool[0], AppLanguage.ID, reverse = true, random = Random(2))!!
        assertEquals(q.options[q.correctIndex], "k1")
        assertEquals("", q.promptTranslit)
    }

    @Test
    fun `question - kolam terlalu kecil tidak crash`() {
        val pool = listOf(entry("a", "x"), entry("b", "y"), entry("c", "z"))
        assertEquals(null, DreamBigGame.question(pool, pool[0], AppLanguage.ID, reverse = false))
    }

    // ---- stars ----

    @Test
    fun `stars - ambang 80, 60, 40 persen`() {
        assertEquals(3, DreamBigGame.stars(8, 10))
        assertEquals(3, DreamBigGame.stars(10, 10))
        assertEquals(2, DreamBigGame.stars(6, 10))
        assertEquals(1, DreamBigGame.stars(4, 10))
        assertEquals(0, DreamBigGame.stars(3, 10))
        assertEquals(0, DreamBigGame.stars(0, 10))
        assertEquals(0, DreamBigGame.stars(0, 0)) // tidak crash
    }

    // ---- unlocked ----

    @Test
    fun `unlocked - day 1 bebas, berikutnya butuh lulus hari sebelumnya`() {
        assertTrue(DreamBigGame.unlocked(1, emptySet()))
        assertFalse(DreamBigGame.unlocked(2, emptySet()))
        assertTrue(DreamBigGame.unlocked(2, setOf(1)))
        assertTrue(DreamBigGame.unlocked(5, setOf(4)))
        assertFalse(DreamBigGame.unlocked(5, setOf(3))) // Day 4 belum lulus
    }
}
