package io.github.zonar23.forkout.ui.workout

import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.relation.WorkoutSetWithExercise
import java.time.Duration
import java.time.Instant

/** One exercise of a running workout with its sets in order. */
data class WorkoutExercise(
    val exercise: ExerciseEntity,
    val position: Int,
    val sets: List<WorkoutSetEntity>,
    /** Completed sets of the last finished workout with this exercise, for orientation. */
    val lastSets: List<WorkoutSetEntity> = emptyList(),
)

/**
 * Rest after the most recently completed set. Based on the set's completion time, so it
 * survives screen lock, app switches and process death.
 */
data class RestTimer(
    val afterSetId: Long,
    val endsAt: Instant,
    val totalSeconds: Int,
)

data class WorkoutSummary(
    val duration: Duration,
    val completedSets: Int,
    val totalSets: Int,
    /** Sum of weight × reps over completed sets, in gram-repetitions (divide by 1000 for kg). */
    val volumeGrams: Long,
    val exercises: List<ExerciseSummary>,
)

data class ExerciseSummary(
    val name: String,
    val completedSets: Int,
    val totalSets: Int,
)

/** Groups sets by exercise position, keeping workout order. */
fun List<WorkoutSetWithExercise>.groupByExercise(): List<WorkoutExercise> =
    groupBy { it.set.exercisePosition }
        .toSortedMap()
        .map { (position, items) ->
            WorkoutExercise(
                exercise = items.first().exercise,
                position = position,
                sets = items.map { it.set }.sortedBy { it.setIndex },
            )
        }

/** The first open set in workout order, which the checklist focuses by default. */
fun List<WorkoutExercise>.firstOpenSet(): WorkoutSetEntity? =
    asSequence().flatMap { it.sets }.firstOrNull { it.completedAt == null }

/**
 * Rest timer for the latest completed set, or null if it has already ended at [now],
 * has no rest, or was skipped by the user.
 */
fun List<WorkoutSetEntity>.currentRest(now: Instant, skippedSetId: Long?): RestTimer? {
    val last = filter { it.completedAt != null }.maxByOrNull { it.completedAt!! } ?: return null
    if (last.restSeconds <= 0 || last.id == skippedSetId) return null
    val endsAt = last.completedAt!!.plusSeconds(last.restSeconds.toLong())
    return if (endsAt.isAfter(now)) RestTimer(last.id, endsAt, last.restSeconds) else null
}

fun summarize(
    startedAt: Instant,
    finishedAt: Instant,
    exercises: List<WorkoutExercise>,
): WorkoutSummary {
    val allSets = exercises.flatMap { it.sets }
    val completed = allSets.filter { it.completedAt != null }
    return WorkoutSummary(
        duration = Duration.between(startedAt, finishedAt),
        completedSets = completed.size,
        totalSets = allSets.size,
        volumeGrams = completed.sumOf { (it.actualWeightGrams ?: 0).toLong() * (it.actualReps ?: 0) },
        exercises = exercises.map { exercise ->
            ExerciseSummary(
                name = exercise.exercise.name,
                completedSets = exercise.sets.count { it.completedAt != null },
                totalSets = exercise.sets.size,
            )
        },
    )
}
