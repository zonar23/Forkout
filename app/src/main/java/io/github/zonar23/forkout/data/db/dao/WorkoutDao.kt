package io.github.zonar23.forkout.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.relation.ExerciseHistoryRow
import io.github.zonar23.forkout.data.db.relation.SessionOverview
import io.github.zonar23.forkout.data.db.relation.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>): List<Long>

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getSet(id: Long): WorkoutSetEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM workout_sessions WHERE finished_at IS NULL)")
    suspend fun hasActiveSession(): Boolean

    /** The workout currently in progress, if any. */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE finished_at IS NULL ORDER BY started_at DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSessionWithSets?>

    @Query("SELECT * FROM workout_sessions WHERE finished_at IS NOT NULL ORDER BY started_at DESC")
    fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE finished_at IS NOT NULL AND started_at >= :since")
    fun observeFinishedCountSince(since: Instant): Flow<Int>

    /** Completed sets of [exerciseId] from the most recent finished workout that contains it. */
    @Query(
        """
        SELECT * FROM workout_sets
        WHERE exercise_id = :exerciseId AND completed_at IS NOT NULL AND session_id = (
            SELECT s.id FROM workout_sessions s
            JOIN workout_sets ws ON ws.session_id = s.id
            WHERE ws.exercise_id = :exerciseId AND ws.completed_at IS NOT NULL
                AND s.finished_at IS NOT NULL
            ORDER BY s.started_at DESC
            LIMIT 1
        )
        ORDER BY set_index
        """
    )
    suspend fun getLastCompletedSets(exerciseId: Long): List<WorkoutSetEntity>

    /** All completed sets of [exerciseId] with their workout's start, newest workout first. */
    @Query(
        """
        SELECT ws.*, s.started_at AS session_started_at FROM workout_sets ws
        JOIN workout_sessions s ON ws.session_id = s.id
        WHERE ws.exercise_id = :exerciseId AND ws.completed_at IS NOT NULL
            AND s.finished_at IS NOT NULL
        ORDER BY s.started_at DESC, ws.set_index
        """
    )
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>>

    /** Finished workouts with set count and volume, newest first. */
    @Query(
        """
        SELECT s.*,
            COUNT(ws.id) AS completed_sets,
            COALESCE(SUM(CAST(ws.actual_weight_grams AS INTEGER) * ws.actual_reps), 0) AS volume_grams
        FROM workout_sessions s
        LEFT JOIN workout_sets ws ON ws.session_id = s.id AND ws.completed_at IS NOT NULL
        WHERE s.finished_at IS NOT NULL
        GROUP BY s.id
        ORDER BY s.started_at DESC
        """
    )
    fun observeSessionOverviews(): Flow<List<SessionOverview>>

    @Query("SELECT * FROM workout_sessions WHERE finished_at IS NOT NULL ORDER BY started_at DESC LIMIT 1")
    fun observeLastFinishedSession(): Flow<WorkoutSessionEntity?>
}
