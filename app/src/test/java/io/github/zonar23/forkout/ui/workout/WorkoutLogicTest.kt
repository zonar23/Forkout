package io.github.zonar23.forkout.ui.workout

import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.db.relation.WorkoutSetWithExercise
import io.github.zonar23.forkout.ui.components.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WorkoutLogicTest {

    private val now = Instant.parse("2026-10-23T10:00:00Z")
    private val legPress = ExerciseEntity(1, "Beinpresse", ExerciseType.MACHINE, setOf(MuscleGroup.LEGS))
    private val latPull = ExerciseEntity(2, "Latzug", ExerciseType.MACHINE, setOf(MuscleGroup.BACK))

    private fun set(
        id: Long,
        exerciseId: Long,
        position: Int,
        index: Int,
        completedAt: Instant? = null,
        reps: Int = 10,
        weight: Int = 50_000,
        rest: Int = 90,
    ) = WorkoutSetEntity(
        id = id,
        sessionId = 1,
        exerciseId = exerciseId,
        exercisePosition = position,
        setIndex = index,
        targetReps = 10,
        targetWeightGrams = 50_000,
        restSeconds = rest,
        actualReps = reps,
        actualWeightGrams = weight,
        completedAt = completedAt,
    )

    @Test
    fun groupByExercise_keepsWorkoutOrder() {
        val sets = listOf(
            WorkoutSetWithExercise(set(3, 2, position = 1, index = 0), latPull),
            WorkoutSetWithExercise(set(2, 1, position = 0, index = 1), legPress),
            WorkoutSetWithExercise(set(1, 1, position = 0, index = 0), legPress),
        )

        val grouped = sets.groupByExercise()

        assertEquals(listOf("Beinpresse", "Latzug"), grouped.map { it.exercise.name })
        assertEquals(listOf(1L, 2L), grouped[0].sets.map { it.id })
    }

    @Test
    fun firstOpenSet_skipsCompleted() {
        val exercises = listOf(
            WorkoutExercise(legPress, 0, listOf(set(1, 1, 0, 0, completedAt = now), set(2, 1, 0, 1))),
            WorkoutExercise(latPull, 1, listOf(set(3, 2, 1, 0))),
        )

        assertEquals(2L, exercises.firstOpenSet()?.id)
        assertNull(listOf(WorkoutExercise(latPull, 0, listOf(set(4, 2, 0, 0, completedAt = now)))).firstOpenSet())
    }

    @Test
    fun currentRest_runsFromLatestCompletedSet() {
        val sets = listOf(
            set(1, 1, 0, 0, completedAt = now.minusSeconds(200)),
            set(2, 1, 0, 1, completedAt = now.minusSeconds(30), rest = 90),
            set(3, 1, 0, 2),
        )

        val rest = sets.currentRest(now, skippedSetId = null)!!

        assertEquals(2L, rest.afterSetId)
        assertEquals(now.plusSeconds(60), rest.endsAt)
        assertEquals(90, rest.totalSeconds)
    }

    @Test
    fun currentRest_isNullWhenEndedSkippedOrWithoutRest() {
        val ended = listOf(set(1, 1, 0, 0, completedAt = now.minusSeconds(120), rest = 90))
        val running = listOf(set(2, 1, 0, 0, completedAt = now.minusSeconds(10), rest = 90))
        val noRest = listOf(set(3, 1, 0, 0, completedAt = now.minusSeconds(10), rest = 0))

        assertNull(ended.currentRest(now, skippedSetId = null))
        assertNull(running.currentRest(now, skippedSetId = 2))
        assertNull(noRest.currentRest(now, skippedSetId = null))
        assertNull(emptyList<WorkoutSetEntity>().currentRest(now, skippedSetId = null))
    }

    @Test
    fun summarize_countsOnlyCompletedSetsForVolume() {
        val exercises = listOf(
            WorkoutExercise(
                legPress, 0,
                listOf(
                    set(1, 1, 0, 0, completedAt = now, reps = 10, weight = 80_000),
                    set(2, 1, 0, 1, completedAt = now, reps = 8, weight = 82_500),
                    set(3, 1, 0, 2, reps = 10, weight = 80_000),
                ),
            ),
            WorkoutExercise(latPull, 1, listOf(set(4, 2, 1, 0, completedAt = now, reps = 12, weight = 40_000))),
        )

        val summary = summarize(now.minusSeconds(3_600), now, exercises)

        assertEquals(Duration.ofHours(1), summary.duration)
        assertEquals(3, summary.completedSets)
        assertEquals(4, summary.totalSets)
        // 10 × 80 + 8 × 82.5 + 12 × 40 = 1940 kg
        assertEquals(1_940_000L, summary.volumeGrams)
        assertEquals(listOf(2, 1), summary.exercises.map { it.completedSets })
    }

    @Test
    fun formatElapsed_addsHoursOnlyWhenNeeded() {
        assertEquals("0:05", TimeFormat.formatElapsed(5))
        assertEquals("12:05", TimeFormat.formatElapsed(725))
        assertEquals("1:02:03", TimeFormat.formatElapsed(3_723))
    }
}
