package io.github.zonar23.forkout.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A performed (or running) workout. [finishedAt] is null while the workout is in progress. */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_day_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("plan_day_id"), Index("started_at")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_day_id") val planDayId: Long?,
    /** Copy of the plan day name, so history stays readable after the plan is deleted. */
    @ColumnInfo(name = "plan_day_name") val planDayName: String,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant? = null,
    val note: String = "",
)

/**
 * One set of a workout. Target values are copied from the plan when the workout starts and
 * never reference plan sets, so later plan changes do not alter history.
 * [completedAt] is null while the set is still open.
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Exercises with history are archived, never deleted.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("session_id"), Index("exercise_id")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    /** Order of the exercise within the workout. */
    @ColumnInfo(name = "exercise_position") val exercisePosition: Int,
    /** Order of the set within its exercise. */
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int?,
    @ColumnInfo(name = "target_weight_grams") val targetWeightGrams: Int?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "actual_reps") val actualReps: Int?,
    @ColumnInfo(name = "actual_weight_grams") val actualWeightGrams: Int?,
    val rir: Int? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
)
