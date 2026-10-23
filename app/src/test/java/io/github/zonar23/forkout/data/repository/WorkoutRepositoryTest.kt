package io.github.zonar23.forkout.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest : DatabaseTest() {

    private val clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC)
    private val plans by lazy { PlanRepository(db, clock) }
    private val workouts by lazy { WorkoutRepository(db, clock) }

    /** Plan day with Beinpresse (3 x 12 @ 80 kg) followed by Latzug (2 x 10 @ 45 kg). */
    private suspend fun planDay(): Long {
        val planId = plans.createPlan("Coach-Plan", sessionsPerWeek = 3)
        val dayId = plans.addDay(planId, "Einheit A")
        val beinpresse = db.exerciseDao().insert(exercise("Beinpresse"))
        val latzug = db.exerciseDao().insert(exercise("Latzug"))
        plans.addExercise(dayId, beinpresse, setCount = 3, targetReps = 12, targetWeightGrams = 80_000, restSeconds = 90)
        plans.addExercise(dayId, latzug, setCount = 2, targetReps = 10, targetWeightGrams = 45_000, restSeconds = 60)
        return dayId
    }

    @Test
    fun start_copiesTargetsAndPrefillsActualValues() = runTest {
        val dayId = planDay()

        val sessionId = workouts.start(dayId)
        val workout = workouts.observeSession(sessionId).first()!!

        assertEquals("Einheit A", workout.session.planDayName)
        assertEquals(clock.instant(), workout.session.startedAt)
        assertEquals(
            listOf("Beinpresse", "Beinpresse", "Beinpresse", "Latzug", "Latzug"),
            workout.sets.map { it.exercise.name },
        )
        assertEquals(listOf(0, 1, 2, 0, 1), workout.sets.map { it.set.setIndex })
        val first = workout.sets.first().set
        assertEquals(12, first.targetReps)
        assertEquals(80_000, first.targetWeightGrams)
        assertEquals(12, first.actualReps)
        assertEquals(80_000, first.actualWeightGrams)
        assertEquals(90, first.restSeconds)
        assertTrue(workout.sets.all { it.set.completedAt == null })
    }

    @Test
    fun planChangesAfterStart_doNotAlterWorkout() = runTest {
        val dayId = planDay()
        val sessionId = workouts.start(dayId)

        val day = plans.observeDay(dayId).first()!!
        plans.updateSets(day.exercises[0].sets.map { it.copy(targetWeightGrams = 100_000) })

        val workout = workouts.observeSession(sessionId).first()!!
        assertEquals(80_000, workout.sets.first().set.targetWeightGrams)
    }

    @Test
    fun start_failsWhileAnotherWorkoutRuns() = runTest {
        val dayId = planDay()
        workouts.start(dayId)

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { workouts.start(dayId) }
        }
    }

    @Test
    fun completeAndFinish_appearsInHistoryWithLastValues() = runTest {
        val sessionId = workouts.start(planDay())
        val firstSet = workouts.observeSession(sessionId).first()!!.sets.first().set

        workouts.updateActual(firstSet.id, reps = 10, weightGrams = 82_500)
        workouts.setCompleted(firstSet.id, completed = true)
        workouts.finish(sessionId)

        assertNull(workouts.observeActive().first())
        assertEquals(1, workouts.observeHistory().first().size)
        val last = workouts.lastCompletedSets(firstSet.exerciseId)
        assertEquals(1, last.size)
        assertEquals(82_500, last[0].actualWeightGrams)
        assertEquals(10, last[0].actualReps)
        assertNotNull(last[0].completedAt)
    }

    @Test
    fun discard_removesWorkout() = runTest {
        val sessionId = workouts.start(planDay())

        workouts.discard(sessionId)

        assertNull(workouts.observeSession(sessionId).first())
        assertNull(workouts.observeActive().first())
    }
}
