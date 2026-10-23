package io.github.zonar23.forkout.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PlanDaoTest : DatabaseTest() {

    private val dao get() = db.planDao()

    private suspend fun insertPlanWithOneDay(): Pair<Long, Long> {
        val planId = dao.insertPlan(
            PlanEntity(name = "Coach-Plan", sessionsPerWeek = 3, createdAt = Instant.now())
        )
        val dayId = dao.insertDay(PlanDayEntity(planId = planId, name = "Einheit A", position = 0))
        return planId to dayId
    }

    @Test
    fun dayWithExercises_containsExerciseAndSets() = runTest {
        val (_, dayId) = insertPlanWithOneDay()
        val exerciseId = db.exerciseDao().insert(exercise("Beinpresse"))
        val planExerciseId = dao.insertPlanExercise(
            PlanExerciseEntity(planDayId = dayId, exerciseId = exerciseId, position = 0)
        )
        dao.insertSets(
            (0..2).map {
                PlanSetEntity(
                    planExerciseId = planExerciseId,
                    position = it,
                    targetReps = 12,
                    targetWeightGrams = 82_500,
                    restSeconds = 90,
                )
            }
        )

        val day = dao.observeDayWithExercises(dayId).first()!!

        assertEquals("Einheit A", day.day.name)
        assertEquals(1, day.exercises.size)
        assertEquals("Beinpresse", day.exercises[0].exercise.name)
        assertEquals(listOf(0, 1, 2), day.exercises[0].sets.map { it.position }.sorted())
        assertEquals(82_500, day.exercises[0].sets[0].targetWeightGrams)
    }

    @Test
    fun deletePlan_cascadesToDaysExercisesAndSets() = runTest {
        val (planId, dayId) = insertPlanWithOneDay()
        val exerciseId = db.exerciseDao().insert(exercise("Latzug"))
        val planExerciseId = dao.insertPlanExercise(
            PlanExerciseEntity(planDayId = dayId, exerciseId = exerciseId, position = 0)
        )
        dao.insertSets(
            listOf(PlanSetEntity(planExerciseId = planExerciseId, position = 0, targetReps = 10, targetWeightGrams = 0, restSeconds = 60))
        )

        val plan = dao.observePlanWithDays(planId).first()!!.plan
        dao.deletePlan(plan)

        assertNull(dao.observePlanWithDays(planId).first())
        assertNull(dao.getDayWithExercises(dayId))
        // The exercise itself stays in the library.
        assertEquals(1, db.exerciseDao().observeAll().first().size)
    }

    @Test
    fun observeActivePlans_excludesArchived() = runTest {
        dao.insertPlan(PlanEntity(name = "Aktiv", sessionsPerWeek = 2, createdAt = Instant.now()))
        dao.insertPlan(
            PlanEntity(name = "Alt", sessionsPerWeek = 2, createdAt = Instant.now(), isArchived = true)
        )

        assertEquals(listOf("Aktiv"), dao.observeActivePlans().first().map { it.name })
    }
}
