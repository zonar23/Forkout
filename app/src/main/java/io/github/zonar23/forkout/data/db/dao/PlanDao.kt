package io.github.zonar23.forkout.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.relation.DayExerciseCount
import io.github.zonar23.forkout.data.db.relation.PlanDayWithExercises
import io.github.zonar23.forkout.data.db.relation.PlanWithDays
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    // Plans

    @Query("SELECT * FROM plans WHERE is_archived = 0 ORDER BY created_at DESC")
    fun observeActivePlans(): Flow<List<PlanEntity>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :id")
    fun observePlanWithDays(id: Long): Flow<PlanWithDays?>

    @Transaction
    @Query("SELECT * FROM plans WHERE is_archived = 0 ORDER BY created_at DESC")
    fun observeActivePlansWithDays(): Flow<List<PlanWithDays>>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlan(id: Long): PlanEntity?

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Delete
    suspend fun deletePlan(plan: PlanEntity)

    // Days

    @Transaction
    @Query("SELECT * FROM plan_days WHERE id = :id")
    fun observeDayWithExercises(id: Long): Flow<PlanDayWithExercises?>

    @Transaction
    @Query("SELECT * FROM plan_days WHERE id = :id")
    suspend fun getDayWithExercises(id: Long): PlanDayWithExercises?

    @Query("SELECT COUNT(*) FROM plan_days WHERE plan_id = :planId")
    suspend fun countDays(planId: Long): Int

    @Query("SELECT * FROM plan_days WHERE plan_id = :planId ORDER BY position")
    suspend fun getDays(planId: Long): List<PlanDayEntity>

    @Query("SELECT * FROM plan_days WHERE id = :id")
    suspend fun getDay(id: Long): PlanDayEntity?

    /** Number of exercises per day of [planId]; days without exercises are missing. */
    @Query(
        """
        SELECT pe.plan_day_id, COUNT(*) AS count FROM plan_exercises pe
        JOIN plan_days d ON d.id = pe.plan_day_id
        WHERE d.plan_id = :planId
        GROUP BY pe.plan_day_id
        """
    )
    fun observeExerciseCounts(planId: Long): Flow<List<DayExerciseCount>>

    @Insert
    suspend fun insertDay(day: PlanDayEntity): Long

    @Update
    suspend fun updateDays(days: List<PlanDayEntity>)

    @Delete
    suspend fun deleteDay(day: PlanDayEntity)

    // Exercises within a day

    @Query("SELECT COUNT(*) FROM plan_exercises WHERE plan_day_id = :dayId")
    suspend fun countPlanExercises(dayId: Long): Int

    @Query("SELECT * FROM plan_exercises WHERE plan_day_id = :dayId ORDER BY position")
    suspend fun getPlanExercises(dayId: Long): List<PlanExerciseEntity>

    @Insert
    suspend fun insertPlanExercise(planExercise: PlanExerciseEntity): Long

    @Update
    suspend fun updatePlanExercises(planExercises: List<PlanExerciseEntity>)

    @Delete
    suspend fun deletePlanExercise(planExercise: PlanExerciseEntity)

    // Sets

    @Query("SELECT * FROM plan_sets WHERE plan_exercise_id = :planExerciseId ORDER BY position")
    suspend fun getSets(planExerciseId: Long): List<PlanSetEntity>

    @Insert
    suspend fun insertSets(sets: List<PlanSetEntity>): List<Long>

    @Update
    suspend fun updateSets(sets: List<PlanSetEntity>)

    @Delete
    suspend fun deleteSet(set: PlanSetEntity)
}
