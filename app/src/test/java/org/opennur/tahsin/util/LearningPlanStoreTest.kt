package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test

class LearningPlanStoreTest {

    private lateinit var directory: File
    private lateinit var file: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("learning-plan-test").toFile()
        file = File(directory, "plan.json")
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `missing and corrupt files return defaults`() {
        assertThat(LearningPlanStore(file).read()).isEqualTo(LearningPlanSnapshot())
        file.writeText("not json")
        assertThat(LearningPlanStore(file).read()).isEqualTo(LearningPlanSnapshot())
    }

    @Test
    fun `mark complete persists and accumulates on same day and goal`() {
        val store = LearningPlanStore(file)

        store.markComplete(10, "recitation", "recite")
        val updated = store.markComplete(10, "recitation", "tajwid")

        assertThat(updated.day).isEqualTo(10)
        assertThat(updated.goalKey).isEqualTo("recitation")
        assertThat(updated.completedKeys).containsExactly("recite", "tajwid")
        assertThat(LearningPlanStore(file).read()).isEqualTo(updated)
    }

    @Test
    fun `new day or goal starts a clean completion set`() {
        val store = LearningPlanStore(file)

        store.markComplete(10, "recitation", "recite")
        val newDay = store.markComplete(11, "recitation", "tajwid")
        assertThat(newDay.completedKeys).containsExactly("tajwid")

        val newGoal = store.markComplete(11, "arabic", "arabic")
        assertThat(newGoal.completedKeys).containsExactly("arabic")
        assertThat(newGoal.goalKey).isEqualTo("arabic")
    }
}
