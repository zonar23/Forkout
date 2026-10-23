package io.github.zonar23.forkout.data.repository

import androidx.room.withTransaction
import io.github.zonar23.forkout.data.db.ForkoutDatabase
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.relation.PlanDayWithExercises
import io.github.zonar23.forkout.data.db.relation.PlanWithDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val db: ForkoutDatabase,
    private val clock: Clock,
) {
    private val dao = db.planDao()

    fun observePlans(): Flow<List<PlanEntity>> = dao.observeActivePlans()

    /** Plan with its days sorted by position. */
    fun observePlan(id: Long): Flow<PlanWithDays?> =
        dao.observePlanWithDays(id).map { plan ->
            plan?.copy(days = plan.days.sortedBy { it.position })
        }

    /** Day with exercises and sets, all sorted by position. */
    fun observeDay(id: Long): Flow<PlanDayWithExercises?> =
        dao.observeDayWithExercises(id).map { it?.sorted() }

    suspend fun createPlan(name: String, sessionsPerWeek: Int, goal: String = "", note: String = ""): Long =
        dao.insertPlan(
            PlanEntity(
                name = name,
                goal = goal,
                sessionsPerWeek = sessionsPerWeek,
                note = note,
                createdAt = clock.instant(),
            )
        )

    suspend fun updatePlan(plan: PlanEntity) = dao.updatePlan(plan)

    suspend fun deletePlan(plan: PlanEntity) = dao.deletePlan(plan)

    /** Appends a new day at the end of the plan. */
    suspend fun addDay(planId: Long, name: String): Long = db.withTransaction {
        dao.insertDay(PlanDayEntity(planId = planId, name = name, position = dao.countDays(planId)))
    }

    /** Persists the given order; positions follow the list index. */
    suspend fun reorderDays(days: List<PlanDayEntity>) =
        dao.updateDays(days.mapIndexed { index, day -> day.copy(position = index) })

    suspend fun deleteDay(day: PlanDayEntity) = dao.deleteDay(day)

    /** Appends [exerciseId] to the day with [setCount] identical sets. */
    suspend fun addExercise(
        dayId: Long,
        exerciseId: Long,
        setCount: Int,
        targetReps: Int,
        targetWeightGrams: Int,
        restSeconds: Int,
    ): Long = db.withTransaction {
        val position = dao.countPlanExercises(dayId)
        val planExerciseId = dao.insertPlanExercise(
            PlanExerciseEntity(planDayId = dayId, exerciseId = exerciseId, position = position)
        )
        dao.insertSets(
            List(setCount) { index ->
                PlanSetEntity(
                    planExerciseId = planExerciseId,
                    position = index,
                    targetReps = targetReps,
                    targetWeightGrams = targetWeightGrams,
                    restSeconds = restSeconds,
                )
            }
        )
        planExerciseId
    }

    suspend fun reorderExercises(planExercises: List<PlanExerciseEntity>) =
        dao.updatePlanExercises(planExercises.mapIndexed { index, item -> item.copy(position = index) })

    suspend fun removeExercise(planExercise: PlanExerciseEntity) = dao.deletePlanExercise(planExercise)

    suspend fun updateSets(sets: List<PlanSetEntity>) = dao.updateSets(sets)

    suspend fun deleteSet(set: PlanSetEntity) = dao.deleteSet(set)

    private fun PlanDayWithExercises.sorted() = copy(
        exercises = exercises
            .sortedBy { it.planExercise.position }
            .map { it.copy(sets = it.sets.sortedBy { set -> set.position }) },
    )
}
