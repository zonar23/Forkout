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
) {
    /** Stable identity, also when the exercise is moved: its first set never changes. */
    val key: Long get() = sets.firstOrNull()?.id ?: -position.toLong()

    val completedCount: Int get() = sets.count { it.completedAt != null }

    val isDone: Boolean get() = sets.isNotEmpty() && completedCount == sets.size

    /** Last time's values for [set]: the set with the same number, else the last one done. */
    fun lastTimeFor(set: WorkoutSetEntity): WorkoutSetEntity? =
        lastSets.firstOrNull { it.setIndex == set.setIndex } ?: lastSets.lastOrNull()
}

/** The set shown large in the focus area, with its context. */
data class Focus(
    val exercise: WorkoutExercise,
    val set: WorkoutSetEntity,
    /** 1-based. */
    val setNumber: Int,
    /** Last time's values, only if they differ from the current ones. */
    val lastTime: WorkoutSetEntity?,
)

fun List<WorkoutExercise>.focus(focusedSetId: Long?): Focus? {
    val exercise = firstOrNull { exercise -> exercise.sets.any { it.id == focusedSetId } } ?: return null
    val set = exercise.sets.first { it.id == focusedSetId }
    val lastTime = exercise.lastTimeFor(set)?.takeIf {
        it.actualReps != set.actualReps || it.actualWeightGrams != set.actualWeightGrams
    }
    return Focus(exercise, set, exercise.sets.indexOf(set) + 1, lastTime)
}

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
