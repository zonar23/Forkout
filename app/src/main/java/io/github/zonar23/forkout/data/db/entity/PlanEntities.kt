package io.github.zonar23.forkout.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: String = "",
    @ColumnInfo(name = "sessions_per_week") val sessionsPerWeek: Int,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
)

/** A training day within a plan, e.g. "Einheit A". */
@Entity(
    tableName = "plan_days",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plan_id")],
)
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_id") val planId: Long,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_day_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Exercises are archived, never deleted while a plan references them.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("plan_day_id"), Index("exercise_id")],
)
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_day_id") val planDayId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val position: Int,
    val note: String = "",
)

@Entity(
    tableName = "plan_sets",
    foreignKeys = [
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plan_exercise_id")],
)
data class PlanSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_exercise_id") val planExerciseId: Long,
    val position: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int,
    @ColumnInfo(name = "target_weight_grams") val targetWeightGrams: Int,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
)
