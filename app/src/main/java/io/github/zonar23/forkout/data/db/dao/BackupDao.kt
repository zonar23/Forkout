package io.github.zonar23.forkout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity

/** Bulk access to all tables for JSON export and import. */
@Dao
interface BackupDao {

    @Query("SELECT * FROM exercises ORDER BY id")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM plans ORDER BY id")
    suspend fun getPlans(): List<PlanEntity>

    @Query("SELECT * FROM plan_days ORDER BY plan_id, position")
    suspend fun getPlanDays(): List<PlanDayEntity>

    @Query("SELECT * FROM plan_exercises ORDER BY plan_day_id, position")
    suspend fun getPlanExercises(): List<PlanExerciseEntity>

    @Query("SELECT * FROM plan_sets ORDER BY plan_exercise_id, position")
    suspend fun getPlanSets(): List<PlanSetEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY started_at")
    suspend fun getSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sets ORDER BY session_id, exercise_position, set_index")
    suspend fun getWorkoutSets(): List<WorkoutSetEntity>

    // Children before parents, so foreign keys never block.
    @Query("DELETE FROM workout_sets")
    suspend fun deleteWorkoutSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteSessions()

    @Query("DELETE FROM plan_sets")
    suspend fun deletePlanSets()

    @Query("DELETE FROM plan_exercises")
    suspend fun deletePlanExercises()

    @Query("DELETE FROM plan_days")
    suspend fun deletePlanDays()

    @Query("DELETE FROM plans")
    suspend fun deletePlans()

    @Query("DELETE FROM exercises")
    suspend fun deleteExercises()

    suspend fun deleteAll() {
        deleteWorkoutSets()
        deleteSessions()
        deletePlanSets()
        deletePlanExercises()
        deletePlanDays()
        deletePlans()
        deleteExercises()
    }

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Insert
    suspend fun insertPlanDays(days: List<PlanDayEntity>)

    @Insert
    suspend fun insertPlanExercise(planExercise: PlanExerciseEntity): Long

    @Insert
    suspend fun insertPlanSets(sets: List<PlanSetEntity>)

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)
}
