package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.opennur.tahsin.data.learning.MemorizationCard

class MemorizationStoreTest {

    private lateinit var directory: File
    private lateinit var file: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("memorization-test").toFile()
        file = File(directory, "memorization.json")
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `read missing and malformed data safely`() {
        assertThat(MemorizationStore(file).read()).isEqualTo(MemorizationSnapshot())
        file.writeText("bad json")
        assertThat(MemorizationStore(file).read()).isEqualTo(MemorizationSnapshot())
    }

    @Test
    fun `write and upsert replace matching card`() {
        val store = MemorizationStore(file)
        val first = MemorizationCard(1, 1)
        val updated = first.copy(intervalDays = 3, dueDay = 4)
        store.write(MemorizationSnapshot(listOf(first, MemorizationCard(1, 2))))

        store.upsert(updated)
        val snapshot = store.upsert(MemorizationCard(2, 1))

        assertThat(snapshot.cards).containsExactly(updated, MemorizationCard(1, 2), MemorizationCard(2, 1))
            .inOrder()
        assertThat(MemorizationStore(file).read()).isEqualTo(snapshot)
    }
}
