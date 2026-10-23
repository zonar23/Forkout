package io.github.zonar23.forkout.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.DatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class WorkoutHistoryTest : DatabaseTest() {

    /** Clock that can be moved forward between workouts. */
    private var now = Instant.parse("2026-10-20T10:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
    }
    private val plans by lazy { PlanRepository(db, clock) }
    private val workouts by lazy { WorkoutRepository(db, clock) }

    private var exerciseId = 0L
    private var dayId = 0L

    private suspend fun setUpDay() {
        val planId = plans.createPlan("Plan", sessionsPerWeek = 3)
        dayId = plans.addDay(planId, "Einheit A")
        exerciseId = db.exerciseDao().insert(exercise("Beinpresse"))
        plans.addExercise(dayId, exerciseId, setCount = 3, targetReps = 10, targetWeightGrams = 80_000, restSeconds = 90)
    }

    /** Runs a workout completing [completed] sets and returns its id. */
    private suspend fun workout(completed: Int, weightGrams: Int = 80_000): Long {
        val id = workouts.start(dayId)
        workouts.observeSession(id).first()!!.sets.take(completed).forEach {
            workouts.updateActual(it.set.id, reps = 10, weightGrams = weightGrams)
            workouts.setCompleted(it.set.id, completed = true)
        }
        now = now.plus(Duration.ofMinutes(45))
        workouts.finish(id)
        now = now.plus(Duration.ofDays(2))
        return id
    }

    @Test
    fun overviews_countCompletedSetsAndVolume_newestFirst() = runTest {
        setUpDay()
        val first = workout(completed = 3)
        val second = workout(completed = 2, weightGrams = 82_500)

        val overviews = workouts.observeOverviews().first()

        assertEquals(listOf(second, first), overviews.map { it.session.id })
        assertEquals(listOf(2, 3), overviews.map { it.completedSets })
        // 2 × 10 × 82.5 kg and 3 × 10 × 80 kg, in gram-repetitions
        assertEquals(listOf(1_650_000L, 2_400_000L), overviews.map { it.volumeGrams })
    }

    @Test
    fun overviews_includeWorkoutsWithoutCompletedSets_andSkipRunningOnes() = runTest {
        setUpDay()
        workout(completed = 0)
        workouts.start(dayId)

        val overviews = workouts.observeOverviews().first()

        assertEquals(1, overviews.size)
        assertEquals(0, overviews[0].completedSets)
        assertEquals(0L, overviews[0].volumeGrams)
    }

    @Test
    fun exerciseHistory_groupsSetsByWorkout() = runTest {
        setUpDay()
        val first = workout(completed = 3)
        val second = workout(completed = 1, weightGrams = 85_000)

        val history = workouts.observeExerciseHistory(exerciseId).first()

        assertEquals(listOf(second, first), history.map { it.sessionId })
        assertEquals(listOf(1, 3), history.map { it.sets.size })
        assertEquals(85_000, history[0].sets[0].actualWeightGrams)
        assertEquals(Instant.parse("2026-10-22T10:45:00Z"), history[0].startedAt)
    }

    @Test
    fun lastFinished_isNewestFinishedWorkout() = runTest {
        setUpDay()
        assertNull(workouts.observeLastFinished().first())

        workout(completed = 1)
        val second = workout(completed = 1)
        workouts.start(dayId)

        assertEquals(second, workouts.observeLastFinished().first()?.id)
    }
}
