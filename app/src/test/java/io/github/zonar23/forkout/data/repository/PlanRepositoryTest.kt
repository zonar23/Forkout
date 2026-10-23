package io.github.zonar23.forkout.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class PlanRepositoryTest : DatabaseTest() {

    private val clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC)
    private val repository by lazy { PlanRepository(db, clock) }

    private val fallbackSet = PlanSetEntity(
        planExerciseId = 0,
        position = 0,
        targetReps = 10,
        targetWeightGrams = 0,
        restSeconds = 90,
    )

    private suspend fun planWithDay(): Pair<Long, Long> {
        val planId = repository.createPlan("Coach-Plan", sessionsPerWeek = 3)
        return planId to repository.addDay(planId, "Einheit A")
    }

    private suspend fun exerciseId(name: String) = db.exerciseDao().insert(exercise(name))

    @Test
    fun addSet_duplicatesLastSet() = runTest {
        val (_, dayId) = planWithDay()
        val planExerciseId = repository.addExercise(dayId, exerciseId("Latzug"), 2, 10, 40_000, 60)
        val last = repository.observeDay(dayId).first()!!.exercises[0].sets.last()
        repository.updateSet(last.copy(targetWeightGrams = 42_500))

        repository.addSet(planExerciseId, fallbackSet)

        val sets = repository.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(listOf(0, 1, 2), sets.map { it.position })
        assertEquals(listOf(40_000, 42_500, 42_500), sets.map { it.targetWeightGrams })
    }

    @Test
    fun addSet_usesFallbackWithoutSets() = runTest {
        val (_, dayId) = planWithDay()
        val planExerciseId = repository.addExercise(dayId, exerciseId("Latzug"), 0, 10, 0, 60)

        repository.addSet(planExerciseId, fallbackSet)

        val sets = repository.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(1, sets.size)
        assertEquals(planExerciseId, sets[0].planExerciseId)
        assertEquals(90, sets[0].restSeconds)
    }

    @Test
    fun applyToAllSets_setsSameValues() = runTest {
        val (_, dayId) = planWithDay()
        val planExerciseId = repository.addExercise(dayId, exerciseId("Beinpresse"), 3, 12, 80_000, 90)

        repository.applyToAllSets(planExerciseId, targetReps = 8, targetWeightGrams = 100_000, restSeconds = 120)

        val sets = repository.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(listOf(8, 8, 8), sets.map { it.targetReps })
        assertEquals(listOf(100_000, 100_000, 100_000), sets.map { it.targetWeightGrams })
        assertEquals(listOf(120, 120, 120), sets.map { it.restSeconds })
    }

    @Test
    fun deleteSet_renumbersRemainingSets() = runTest {
        val (_, dayId) = planWithDay()
        repository.addExercise(dayId, exerciseId("Beinpresse"), 3, 12, 80_000, 90)
        val middle = repository.observeDay(dayId).first()!!.exercises[0].sets[1]

        repository.deleteSet(middle)

        val sets = repository.observeDay(dayId).first()!!.exercises[0].sets
        assertEquals(listOf(0, 1), sets.map { it.position })
    }

    @Test
    fun removeExerciseAndDeleteDay_renumberPositions() = runTest {
        val (planId, dayA) = planWithDay()
        val dayB = repository.addDay(planId, "Einheit B")
        repository.addDay(planId, "Einheit C")
        repository.addExercise(dayA, exerciseId("Latzug"), 1, 10, 0, 60)
        repository.addExercise(dayA, exerciseId("Beinpresse"), 1, 10, 0, 60)
        repository.addExercise(dayA, exerciseId("Brustpresse"), 1, 10, 0, 60)

        val first = repository.observeDay(dayA).first()!!.exercises[0].planExercise
        repository.removeExercise(first)
        repository.deleteDay(repository.observePlan(planId).first()!!.days.single { it.id == dayB })

        val exercises = repository.observeDay(dayA).first()!!.exercises
        assertEquals(listOf("Beinpresse", "Brustpresse"), exercises.map { it.exercise.name })
        assertEquals(listOf(0, 1), exercises.map { it.planExercise.position })
        val days = repository.observePlan(planId).first()!!.days
        assertEquals(listOf("Einheit A", "Einheit C"), days.map { it.name })
        assertEquals(listOf(0, 1), days.map { it.position })
    }

    @Test
    fun reorderDays_persistsNewOrder() = runTest {
        val (planId, _) = planWithDay()
        repository.addDay(planId, "Einheit B")
        val days = repository.observePlan(planId).first()!!.days

        repository.reorderDays(days.reversed())

        assertEquals(listOf("Einheit B", "Einheit A"), repository.observePlan(planId).first()!!.days.map { it.name })
    }

    @Test
    fun copyDay_appendsDeepCopy() = runTest {
        val (planId, dayId) = planWithDay()
        repository.addExercise(dayId, exerciseId("Latzug"), 2, 10, 45_000, 60)

        val copyId = repository.copyDay(dayId, "Einheit A (Kopie)")
        val copy = repository.observeDay(copyId).first()!!

        assertEquals("Einheit A (Kopie)", copy.day.name)
        assertEquals(1, copy.day.position)
        assertEquals("Latzug", copy.exercises.single().exercise.name)
        assertEquals(listOf(45_000, 45_000), copy.exercises.single().sets.map { it.targetWeightGrams })
        assertEquals(2, repository.observePlan(planId).first()!!.days.size)
    }

    @Test
    fun copyPlan_copiesEverythingIndependently() = runTest {
        val (planId, dayId) = planWithDay()
        repository.addExercise(dayId, exerciseId("Latzug"), 2, 10, 45_000, 60)

        val copyId = repository.copyPlan(planId, "Coach-Plan (Kopie)")
        val copyDayId = repository.observePlan(copyId).first()!!.days.single().id
        val originalSet = repository.observeDay(dayId).first()!!.exercises[0].sets[0]
        repository.updateSet(originalSet.copy(targetWeightGrams = 50_000))

        val copiedSets = repository.observeDay(copyDayId).first()!!.exercises[0].sets
        assertEquals("Coach-Plan (Kopie)", repository.getPlan(copyId)!!.name)
        assertEquals(listOf(45_000, 45_000), copiedSets.map { it.targetWeightGrams })
        assertEquals(2, repository.observePlans().first().size)
    }
}
