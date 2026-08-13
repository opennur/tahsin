package org.opennur.tahsin.util

import org.opennur.tahsin.data.vocab.VocabCard
import org.opennur.tahsin.data.vocab.VocabDaily
import org.opennur.tahsin.data.vocab.VocabState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tes penyimpanan progres kosa kata ([VocabularyStatsStore]) — file di
 * direktori temp (bukan Android Context) lewat konstruktor internal.
 */
class VocabularyStatsStoreTest {

    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("vocab-stats-test").toFile()
        file = File(dir, "vocab-stats.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store() = VocabularyStatsStore(file)

    @Test
    fun `read - file belum ada - keadaan default`() {
        val state = store().read()
        assertTrue(state.cards.isEmpty())
        assertEquals(VocabDaily(), state.daily)
    }

    @Test
    fun `write & read - roundtrip lintas instance`() {
        val state = VocabState(
            cards = mapOf(
                "من" to VocabCard("من", box = 2, nextDue = 999L, correctCount = 3, wrongCount = 1),
                "الله" to VocabCard("الله", box = 0, nextDue = 0L),
            ),
            daily = VocabDaily(date = "2026-06-18", studied = 5, learned = 3),
        )
        store().write(state)
        val loaded = store().read() // instance baru → baca dari disk
        assertEquals(state, loaded)
    }

    @Test
    fun `read - file rusak - default (tidak crash)`() {
        file.writeText("bukan json {")
        val state = store().read()
        assertTrue(state.cards.isEmpty())
    }

    @Test
    fun `clear - hapus file`() {
        store().write(VocabState(cards = mapOf("من" to VocabCard("من"))))
        assertTrue(file.exists())
        store().clear()
        assertTrue(!file.exists())
        assertTrue(store().read().cards.isEmpty())
    }

    @Test
    fun `write - menimpa keadaan lama`() {
        val s = store()
        s.write(VocabState(cards = mapOf("من" to VocabCard("من", box = 1))))
        s.write(VocabState(cards = mapOf("الله" to VocabCard("الله", box = 2))))
        assertEquals(mapOf("الله" to VocabCard("الله", box = 2)), s.read().cards)
    }


    @Test
    fun `write - target berupa direktori - tidak crash`() {
        file.mkdirs()
        java.io.File(file, "placeholder").writeText("x") // dir tidak kosong → renameTo pasti gagal
        store().write(VocabState())
        assertTrue(file.isDirectory)
    }


    @Test
    fun `read - json literal null - keadaan default`() {
        file.writeText("null")
        assertEquals(VocabState(), store().read())
    }


    @Test
    fun `writeDirect - file biasa - tersimpan`() {
        store().writeDirect("""{"xp":1}""")
        assertTrue(file.readText().contains("1"))
    }
}
