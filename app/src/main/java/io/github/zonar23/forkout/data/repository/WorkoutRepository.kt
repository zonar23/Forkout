package io.github.zonar23.forkout.data.repository

import androidx.room.withTransaction
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.db.relation.SessionOverview
import io.github.zonar23.forkout.data.db.relation.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** All completed sets of one exercise in one finished workout. */
data class ExerciseHistoryEntry(
    val sessionId: Long,
    val startedAt: Instant,
    val sets: List<WorkoutSetEntity>,
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val db: ForkoutDatabase,
    private val clock: Clock,
) {
    private val dao = db.workoutDao()
    private val planDao = db.planDao()

    /** The running workout with sets sorted by exercise and set order. */
    fun observeActive(): Flow<WorkoutSessionWithSets?> = dao.observeActiveSession().map { it?.sorted() }

    fun observeSession(id: Long): Flow<WorkoutSessionWithSets?> = dao.observeSession(id).map { it?.sorted() }

    fun observeHistory(): Flow<List<WorkoutSessionEntity>> = dao.observeFinishedSessions()

    /** Finished workouts with completed set count and volume, newest first. */
    fun observeOverviews(): Flow<List<SessionOverview>> = dao.observeSessionOverviews()

    fun observeLastFinished(): Flow<WorkoutSessionEntity?> = dao.observeLastFinishedSession()

    /** Completed sets of an exercise grouped by workout, newest workout first. */
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryEntry>> =
        dao.observeExerciseHistory(exerciseId).map { rows ->
            rows.groupBy { it.set.sessionId }
                .map { (sessionId, sessionRows) ->
                    ExerciseHistoryEntry(
                        sessionId = sessionId,
                        startedAt = sessionRows.first().sessionStartedAt,
                        sets = sessionRows.map { it.set },
                    )
                }
        }

    fun observeFinishedCountSince(since: Instant): Flow<Int> = dao.observeFinishedCountSince(since)

    suspend fun lastCompletedSets(exerciseId: Long): List<WorkoutSetEntity> =
        dao.getLastCompletedSets(exerciseId)

    /**
     * Starts a workout for a plan day. All target values are copied into the workout, and the
     * actual values are prefilled with the targets.
     *
     * @throws IllegalStateException if another workout is still running or the day does not exist.
     */
    suspend fun start(planDayId: Long): Long = db.withTransaction {
        check(!dao.hasActiveSession()) { "Another workout is still in progress" }
        val day = checkNotNull(planDao.getDayWithExercises(planDayId)) { "Plan day $planDayId not found" }

        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                planDayId = day.day.id,
                planDayName = day.day.name,
                startedAt = clock.instant(),
            )
        )
        val sets = day.exercises
            .sortedBy { it.planExercise.position }
            .flatMapIndexed { exercisePosition, item ->
                item.sets.sortedBy { it.position }.mapIndexed { setIndex, planSet ->
                    WorkoutSetEntity(
                        sessionId = sessionId,
                        exerciseId = item.exercise.id,
                        exercisePosition = exercisePosition,
                        setIndex = setIndex,
                        targetReps = planSet.targetReps,
                        targetWeightGrams = planSet.targetWeightGrams,
                        restSeconds = planSet.restSeconds,
                        actualReps = planSet.targetReps,
                        actualWeightGrams = planSet.targetWeightGrams,
                    )
                }
            }
        dao.insertSets(sets)
        sessionId
    }

    /** Updates the actual values of a set without changing its completion state. */
    suspend fun updateActual(setId: Long, reps: Int?, weightGrams: Int?, rir: Int? = null) {
        val set = dao.getSet(setId) ?: return
        dao.updateSet(set.copy(actualReps = reps, actualWeightGrams = weightGrams, rir = rir))
    }

    /** Postpones an exercise, e.g. when its machine is taken. The plan stays unchanged. */
    suspend fun moveExerciseToEnd(sessionId: Long, exercisePosition: Int) =
        dao.moveExerciseToEnd(sessionId, exercisePosition)

    suspend fun setCompleted(setId: Long, completed: Boolean) {
        val set = dao.getSet(setId) ?: return
        dao.updateSet(set.copy(completedAt = if (completed) clock.instant() else null))
    }

    suspend fun finish(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        if (session.finishedAt == null) {
            dao.updateSession(session.copy(finishedAt = clock.instant()))
        }
    }

    /** Cancels a workout and removes it including all sets. */
    suspend fun discard(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        dao.deleteSession(session)
    }

    private fun WorkoutSessionWithSets.sorted() = copy(
        sets = sets.sortedWith(compareBy({ it.set.exercisePosition }, { it.set.setIndex })),
    )
}
