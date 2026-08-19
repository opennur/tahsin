@file:Suppress("MaxLineLength")

package org.opennur.tahsin.util

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionLedgerTest {
    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("question-ledger-test").toFile()
        file = File(dir, "question-history.json")
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `select mengambil soal baru tanpa duplikasi`() {
        val ids = listOf("a", "b", "c", "d")
        val first = QuestionLedger.select("quiz", ids, QuestionLedgerState(), 2, 10, Random(1))
        val state = QuestionLedger.reserve("quiz", ids, QuestionLedgerState(), 2, 10, Random(1)).first
        val second = QuestionLedger.select("quiz", ids, state, 2, 10, Random(2))
        assertEquals(2, first.size)
        assertEquals(2, second.size)
        assertTrue(first.intersect(second.toSet()).isEmpty())
    }

    @Test
    fun `pool yang habis memulai siklus berikutnya dan soal salah direview`() {
        val ids = listOf("a", "b")
        val firstStateAndIds = QuestionLedger.reserve("quiz", ids, QuestionLedgerState(), 2, 10, Random(1))
        val recorded = QuestionLedger.record("quiz", "a", firstStateAndIds.first, false, 10)
        val due = QuestionLedger.select("quiz", ids, recorded, 1, 11, Random(2))
        assertEquals(listOf("a"), due)
        val next = QuestionLedger.reserve("quiz", ids, firstStateAndIds.first, 2, 10, Random(3)).second
        assertEquals(2, next.size)
        assertTrue(next.toSet() == ids.toSet())
        assertEquals(2, QuestionLedger.select("quiz", ids, firstStateAndIds.first, 2, 10, Random(3)).size)
        val exhausted = QuestionLedgerState(
            cycles = mapOf("quiz" to 0),
            exposures = ids.associate { it to QuestionExposure(cycle = 0) }
                .mapKeys { QuestionKey("quiz", it.key).storageKey },
        )
        assertEquals(2, QuestionLedger.select("quiz", ids, exhausted, 2, 10).size)
        assertEquals(2, QuestionLedger.reserve("quiz", ids, exhausted, 2, 10).second.size)
    }

    @Test
    fun `seleksi mencakup due penuh fresh parsial dan siklus lebih baru`() {
        val state = QuestionLedgerState(
            cycles = mapOf("quiz" to 1),
            exposures = mapOf(
                QuestionKey("quiz", "a").storageKey to QuestionExposure(cycle = 1, lastCorrect = false, nextReviewDay = 1),
                QuestionKey("quiz", "b").storageKey to QuestionExposure(cycle = 1, lastCorrect = false, nextReviewDay = 99),
                QuestionKey("quiz", "c").storageKey to QuestionExposure(cycle = 0),
                QuestionKey("quiz", "d").storageKey to QuestionExposure(cycle = 2),
            ),
        )
        assertEquals(listOf("a"), QuestionLedger.select("quiz", listOf("a", "b"), state, 1, 2, Random(1)))
        assertEquals(2, QuestionLedger.select("quiz", listOf("a", "b", "c", "d"), state, 2, 50, Random(1)).size)
        val reserved = QuestionLedger.reserve("quiz", listOf("d"), state, 1, 50, Random(1)).first
        assertEquals(2, reserved.exposures[QuestionKey("quiz", "d").storageKey]?.cycle)
        assertEquals(1, QuestionLedger.record("quiz", "new", QuestionLedgerState(), true, 1).exposures.size)
    }

    @Test
    fun `input kosong dan count nol aman`() {
        assertTrue(QuestionLedger.select("x", emptyList(), QuestionLedgerState(), 4, 1).isEmpty())
        assertTrue(QuestionLedger.select("x", listOf("a"), QuestionLedgerState(), 0, 1).isEmpty())
        assertFalse(QuestionKey("x", "a").storageKey.isBlank())
    }

    @Test
    fun `store roundtrip dan fallback rusak`() {
        val store = QuestionExposureStore(file)
        assertEquals(QuestionLedgerState(), store.read())
        val selected = store.reserve("quiz", listOf("a", "b"), 1, 1, Random(1))
        assertEquals(1, selected.size)
        store.record("quiz", selected.single(), true, 1)
        assertEquals(1, store.read().exposures.values.first().correctCount)
        file.writeText("bukan json")
        assertEquals(QuestionLedgerState(), store.read())
        file.writeText("null")
        assertEquals(QuestionLedgerState(), store.read())
    }

    @Test
    fun `store menangani context dan target file invalid`() {
        val context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = dir
        }
        assertEquals(QuestionLedgerState(), QuestionExposureStore.fromContext(context).read())
        val invalidParent = File(dir, "invalid-parent").apply { writeText("x") }
        assertEquals(
            1,
            QuestionExposureStore(File(invalidParent, "history.json"))
                .reserve("quiz", listOf("a"), 1, 1, Random(1)).size,
        )
        val targetDir = File(dir, "target-dir").apply { mkdirs() }
        File(targetDir, "placeholder").writeText("x")
        QuestionExposureStore(targetDir).reserve("quiz", listOf("a"), 1, 1, Random(1))
    }
}
