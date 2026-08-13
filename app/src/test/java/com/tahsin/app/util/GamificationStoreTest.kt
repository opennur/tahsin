package com.tahsin.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/** Tes ekonomi game (XP/level/streak) — file di direktori temp. */
class GamificationStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("gamification-test").toFile()
        file = File(dir, "gamification.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = GamificationStore(file)

    // ---- level curve ----

    @Test
    fun `level - batas threshold xp`() {
        assertEquals(1, Gamification.levelFor(0))
        assertEquals(1, Gamification.levelFor(99))
        assertEquals(2, Gamification.levelFor(100))
        assertEquals(2, Gamification.levelFor(399))
        assertEquals(3, Gamification.levelFor(400))
        assertEquals(3, Gamification.levelFor(899))
        assertEquals(4, Gamification.levelFor(900))
    }

    @Test
    fun `xpForLevel - 0, 100, 400, 900`() {
        assertEquals(0, Gamification.xpForLevel(1))
        assertEquals(100, Gamification.xpForLevel(2))
        assertEquals(400, Gamification.xpForLevel(3))
        assertEquals(900, Gamification.xpForLevel(4))
    }

    @Test
    fun `progressWithinLevel - separuh di tengah level 2`() {
        assertEquals(0.5f, Gamification.progressWithinLevel(250), 0.001f)
        assertEquals(0f, Gamification.progressWithinLevel(0))
        assertEquals(0f, Gamification.progressWithinLevel(100))
    }

    // ---- streak & todayXp ----

    @Test
    fun `withActivity - hari pertama - streak 1`() {
        val s = Gamification.withActivity(GamificationStats(), 5, today = 100)
        assertEquals(GamificationStats(xp = 5, todayXp = 5, lastActiveDay = 100, streak = 1), s)
    }

    @Test
    fun `withActivity - hari yang sama - streak bertahan, todayXp menumpuk`() {
        val d1 = Gamification.withActivity(GamificationStats(), 5, today = 100)
        val d2 = Gamification.withActivity(d1, 10, today = 100)
        assertEquals(15, d2.xp)
        assertEquals(15, d2.todayXp)
        assertEquals(1, d2.streak)
    }

    @Test
    fun `withActivity - hari berikutnya - streak +1, todayXp reset`() {
        val d1 = Gamification.withActivity(GamificationStats(), 5, today = 100)
        val d2 = Gamification.withActivity(d1, 10, today = 101)
        assertEquals(15, d2.xp)
        assertEquals(10, d2.todayXp)
        assertEquals(2, d2.streak)
    }

    @Test
    fun `withActivity - jeda lebih dari sehari - streak reset ke 1`() {
        val d1 = Gamification.withActivity(GamificationStats(), 5, today = 100)
        val d2 = Gamification.withActivity(d1, 10, today = 103)
        assertEquals(15, d2.xp)
        assertEquals(10, d2.todayXp)
        assertEquals(1, d2.streak)
    }

    // ---- persistence ----

    @Test
    fun `write & read - roundtrip lintas instance`() {
        store().write(
            GamificationStats(
                xp = 250,
                todayXp = 30,
                lastActiveDay = 42,
                streak = 3,
                badges = listOf("first-step"),
            ),
        )
        assertEquals(
            GamificationStats(xp = 250, todayXp = 30, lastActiveDay = 42, streak = 3, badges = listOf("first-step")),
            store().read(),
        )
        assertFalse(file.readText().isBlank())
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json{{{")
        assertEquals(GamificationStats(), store().read())
    }

    @Test
    fun `recordActivity - deteksi naik level`() {
        store().write(GamificationStats(xp = 95, todayXp = 0, lastActiveDay = 0, streak = 0))
        val r1 = store().recordActivity(10, today = LocalDate.of(2026, 1, 1))
        assertTrue(r1.leveledUp) // 95 → 105: level 1 → 2
        assertEquals(2, Gamification.levelFor(r1.after.xp))
        assertEquals(105, r1.after.xp)
        val r2 = store().recordActivity(5, today = LocalDate.of(2026, 1, 1))
        assertFalse(r2.leveledUp)
        assertEquals(110, r2.after.xp)
        assertEquals(15, r2.after.todayXp)
    }

    @Test
    fun `recordActivity - xp nol tetap mencatat kehadiran hari itu`() {
        store().write(GamificationStats(xp = 50, lastActiveDay = 7, streak = 3))
        val r = store().recordActivity(0, today = LocalDate.ofEpochDay(8))
        assertEquals(GamificationStats(xp = 50, todayXp = 0, lastActiveDay = 8, streak = 4), store().read())
        assertFalse(r.leveledUp)
    }

    @Test
    fun `recordActivity - konkuren lintas instance - tidak ada XP hilang`() {
        // Setiap pemanggil membuat instance store baru; RMW harus diserialkan
        // oleh kunci global, bukan synchronized(this) per instance.
        val pool = java.util.concurrent.Executors.newFixedThreadPool(8)
        val latch = java.util.concurrent.CountDownLatch(1)
        val futures = (1..20).map {
            pool.submit(java.util.concurrent.Callable {
                latch.await()
                GamificationStore(file).recordActivity(5, LocalDate.ofEpochDay(100))
            })
        }
        latch.countDown()
        futures.forEach { it.get() }
        pool.shutdown()
        val s = store().read()
        assertEquals(20 * 5, s.xp)
        assertEquals(20 * 5, s.todayXp)
        assertEquals(1, s.streak)
    }

    // ---- todayXpFor (daily goal display) ----

    @Test
    fun `todayXpFor - hari aktif sama - tampilkan todayXp`() {
        val stats = GamificationStats(xp = 100, todayXp = 30, lastActiveDay = 5, streak = 2)
        assertEquals(30, Gamification.todayXpFor(stats, today = 5))
    }

    @Test
    fun `todayXpFor - hari lain - 0 (progres hari ini belum mulai)`() {
        val stats = GamificationStats(xp = 100, todayXp = 30, lastActiveDay = 5, streak = 2)
        assertEquals(0, Gamification.todayXpFor(stats, today = 6))
        assertEquals(0, Gamification.todayXpFor(stats, today = 4))
    }
}
