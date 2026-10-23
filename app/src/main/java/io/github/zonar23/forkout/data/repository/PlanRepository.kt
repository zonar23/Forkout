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

    /** Active plans, newest first, each with days sorted by position. */
    fun observePlansWithDays(): Flow<List<PlanWithDays>> =
        dao.observeActivePlansWithDays().map { plans ->
            plans.map { it.copy(days = it.days.sortedBy { day -> day.position }) }
        }

    suspend fun getDay(id: Long): PlanDayEntity? = dao.getDay(id)

    /** Day with exercises and sets, all sorted by position. */
    fun observeDay(id: Long): Flow<PlanDayWithExercises?> =
        dao.observeDayWithExercises(id).map { it?.sorted() }

    /** Exercise count per day id of [planId]. */
    fun observeExerciseCounts(planId: Long): Flow<Map<Long, Int>> =
        dao.observeExerciseCounts(planId).map { counts -> counts.associate { it.dayId to it.count } }

    suspend fun getPlan(id: Long): PlanEntity? = dao.getPlan(id)

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

    /** Copies a plan with all days, exercises and sets. Returns the new plan id. */
    suspend fun copyPlan(planId: Long, newName: String): Long = db.withTransaction {
        val plan = checkNotNull(dao.getPlan(planId)) { "Plan $planId not found" }
        val newPlanId = dao.insertPlan(plan.copy(id = 0, name = newName, createdAt = clock.instant()))
        dao.getDays(planId).forEach { day -> copyDayInto(day.id, newPlanId, day.name, day.position) }
        newPlanId
    }

    // Days

    /** Appends a new day at the end of the plan. */
    suspend fun addDay(planId: Long, name: String): Long = db.withTransaction {
        dao.insertDay(PlanDayEntity(planId = planId, name = name, position = dao.countDays(planId)))
    }

    suspend fun renameDay(day: PlanDayEntity, name: String) = dao.updateDays(listOf(day.copy(name = name)))

    /** Copies a day with all exercises and sets to the end of the same plan. */
    suspend fun copyDay(dayId: Long, newName: String): Long = db.withTransaction {
        val day = checkNotNull(dao.getDay(dayId)) { "Day $dayId not found" }
        copyDayInto(dayId, day.planId, newName, dao.countDays(day.planId))
    }

    /** Persists the given order; positions follow the list index. */
    suspend fun reorderDays(days: List<PlanDayEntity>) =
        dao.updateDays(days.mapIndexed { index, day -> day.copy(position = index) })

    suspend fun deleteDay(day: PlanDayEntity) = db.withTransaction {
        dao.deleteDay(day)
        reorderDays(dao.getDays(day.planId))
    }

    // Exercises within a day

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

    suspend fun removeExercise(planExercise: PlanExerciseEntity) = db.withTransaction {
        dao.deletePlanExercise(planExercise)
        reorderExercises(dao.getPlanExercises(planExercise.planDayId))
    }

    // Sets

    /** Appends a copy of the last set, or [fallback] when the exercise has no sets yet. */
    suspend fun addSet(planExerciseId: Long, fallback: PlanSetEntity): Long = db.withTransaction {
        val sets = dao.getSets(planExerciseId)
        val template = sets.lastOrNull() ?: fallback
        dao.insertSets(
            listOf(template.copy(id = 0, planExerciseId = planExerciseId, position = sets.size))
        ).single()
    }

    suspend fun updateSet(set: PlanSetEntity) = dao.updateSets(listOf(set))

    suspend fun updateSets(sets: List<PlanSetEntity>) = dao.updateSets(sets)

    /** Gives every set of the exercise the same target values. */
    suspend fun applyToAllSets(planExerciseId: Long, targetReps: Int, targetWeightGrams: Int, restSeconds: Int) =
        db.withTransaction {
            dao.updateSets(
                dao.getSets(planExerciseId).map {
                    it.copy(targetReps = targetReps, targetWeightGrams = targetWeightGrams, restSeconds = restSeconds)
                }
            )
        }

    suspend fun deleteSet(set: PlanSetEntity) = db.withTransaction {
        dao.deleteSet(set)
        dao.updateSets(dao.getSets(set.planExerciseId).mapIndexed { index, item -> item.copy(position = index) })
    }

    /** Must run inside a transaction. */
    private suspend fun copyDayInto(sourceDayId: Long, planId: Long, name: String, position: Int): Long {
        val source = checkNotNull(dao.getDayWithExercises(sourceDayId)).sorted()
        val newDayId = dao.insertDay(PlanDayEntity(planId = planId, name = name, position = position))
        source.exercises.forEach { item ->
            val newPlanExerciseId = dao.insertPlanExercise(item.planExercise.copy(id = 0, planDayId = newDayId))
            dao.insertSets(item.sets.map { it.copy(id = 0, planExerciseId = newPlanExerciseId) })
        }
        return newDayId
    }

    private fun PlanDayWithExercises.sorted() = copy(
        exercises = exercises
            .sortedBy { it.planExercise.position }
            .map { it.copy(sets = it.sets.sortedBy { set -> set.position }) },
    )
}
