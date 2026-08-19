package org.opennur.tahsin.data.learning

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearningPlanTest {

    @Test
    fun `goal keys round trip and unknown falls back to recitation`() {
        LearningGoal.entries.forEach { goal ->
            assertThat(LearningGoal.fromKey(goal.key)).isEqualTo(goal)
        }
        assertThat(LearningGoal.fromKey("unknown")).isEqualTo(LearningGoal.RECITATION)
    }

    @Test
    fun `task keys round trip and unknown returns null`() {
        LearningTaskType.entries.forEach { type ->
            assertThat(LearningTaskType.fromKey(type.key)).isEqualTo(type)
        }
        assertThat(LearningTaskType.fromKey("unknown")).isNull()
    }

    @Test
    fun `each learning goal gets its configured plan`() {
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.RECITATION)).containsExactly(
            LearningTaskType.RECITE,
            LearningTaskType.TAJWID,
            LearningTaskType.VOCABULARY,
        ).inOrder()
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.UNDERSTANDING)).containsExactly(
            LearningTaskType.UNDERSTAND,
            LearningTaskType.VOCABULARY,
            LearningTaskType.RECITE,
        ).inOrder()
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.MEMORIZATION)).containsExactly(
            LearningTaskType.MEMORIZATION,
            LearningTaskType.RECITE,
            LearningTaskType.TAJWID,
        ).inOrder()
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.ARABIC)).containsExactly(
            LearningTaskType.ARABIC,
            LearningTaskType.NAHWU,
            LearningTaskType.SHOROF,
            LearningTaskType.VOCABULARY,
        ).inOrder()
    }

    @Test
    fun `build marks matching tasks and exposes progress`() {
        val plan = LearningPlanEngine.build(
            day = 42,
            goal = LearningGoal.RECITATION,
            completedKeys = setOf(LearningTaskType.RECITE.key),
        )

        assertThat(plan.day).isEqualTo(42)
        assertThat(plan.goal).isEqualTo(LearningGoal.RECITATION)
        assertThat(plan.completedCount).isEqualTo(1)
        assertThat(plan.totalCount).isEqualTo(3)
        assertThat(plan.isComplete).isFalse()
        assertThat(plan.tasks.map { it.order }).containsExactly(0, 1, 2).inOrder()
        assertThat(plan.tasks.first().completed).isTrue()
        assertThat(plan.tasks[1].completed).isFalse()
    }

    @Test
    fun `empty plan is not complete and all complete plan is complete`() {
        assertThat(
            DailyLearningPlan(day = 1, goal = LearningGoal.RECITATION, tasks = emptyList()).isComplete,
        ).isFalse()
        assertThat(
            DailyLearningPlan(day = 1, goal = LearningGoal.RECITATION, tasks = emptyList()).nextTask,
        ).isNull()

        val plan = LearningPlanEngine.build(
            day = 1,
            goal = LearningGoal.ARABIC,
            completedKeys = LearningPlanEngine.taskTypesFor(LearningGoal.ARABIC).map { it.key }.toSet(),
        )
        assertThat(plan.isComplete).isTrue()
        assertThat(plan.completedCount).isEqualTo(plan.totalCount)
        assertThat(plan.nextTask).isNull()
    }

    @Test
    fun `daily minutes limit the plan and next task follows completion`() {
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.RECITATION, dailyMinutes = 5))
            .containsExactly(LearningTaskType.RECITE)
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.RECITATION, dailyMinutes = 15))
            .containsExactly(LearningTaskType.RECITE, LearningTaskType.TAJWID)
            .inOrder()
        assertThat(LearningPlanEngine.taskTypesFor(LearningGoal.ARABIC, dailyMinutes = 30))
            .hasSize(4)
        assertThat(
            LearningPlanEngine.build(day = 1, goal = LearningGoal.RECITATION, dailyMinutes = 5)
                .tasks.first().completed,
        ).isFalse()

        val plan = LearningPlanEngine.build(
            day = 1,
            goal = LearningGoal.RECITATION,
            completedKeys = setOf(LearningTaskType.RECITE.key),
            dailyMinutes = 15,
        )
        assertThat(plan.nextTask?.type).isEqualTo(LearningTaskType.TAJWID)
    }
}
