package io.github.zonar23.forkout.data.db.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity

// Room does not guarantee the order of @Relation children; callers sort by position.

data class PlanWithDays(
    @Embedded val plan: PlanEntity,
    @Relation(parentColumn = "id", entityColumn = "plan_id")
    val days: List<PlanDayEntity>,
)

data class DayExerciseCount(
    @ColumnInfo(name = "plan_day_id") val dayId: Long,
    val count: Int,
)

data class PlanExerciseWithDetails(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "plan_exercise_id")
    val sets: List<PlanSetEntity>,
)

data class PlanDayWithExercises(
    @Embedded val day: PlanDayEntity,
    @Relation(
        entity = PlanExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "plan_day_id",
    )
    val exercises: List<PlanExerciseWithDetails>,
)

data class WorkoutSetWithExercise(
    @Embedded val set: WorkoutSetEntity,
    @Relation(parentColumn = "exercise_id", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class WorkoutSessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = WorkoutSetEntity::class,
        parentColumn = "id",
        entityColumn = "session_id",
    )
    val sets: List<WorkoutSetWithExercise>,
)
