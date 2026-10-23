package io.github.zonar23.forkout.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest : DatabaseTest() {

    private val dao get() = db.workoutDao()
    private val now = Instant.parse("2026-10-23T10:00:00Z")

    private fun set(
        sessionId: Long,
        exerciseId: Long,
        setIndex: Int,
        actualWeightGrams: Int,
        completedAt: Instant? = now,
    ) = WorkoutSetEntity(
        sessionId = sessionId,
        exerciseId = exerciseId,
        exercisePosition = 0,
        setIndex = setIndex,
        targetReps = 10,
        targetWeightGrams = 40_000,
        restSeconds = 90,
        actualReps = 10,
        actualWeightGrams = actualWeightGrams,
        completedAt = completedAt,
    )

    private suspend fun finishedSession(startedAt: Instant, name: String = "Einheit A") =
        dao.insertSession(
            WorkoutSessionEntity(
                planDayId = null,
                planDayName = name,
                startedAt = startedAt,
                finishedAt = startedAt.plus(1, ChronoUnit.HOURS),
            )
        )

    @Test
    fun activeSession_isUnfinishedOne() = runTest {
        finishedSession(now.minus(2, ChronoUnit.DAYS))
        val activeId = dao.insertSession(
            WorkoutSessionEntity(planDayId = null, planDayName = "Einheit B", startedAt = now)
        )

        assertEquals(activeId, dao.observeActiveSession().first()?.session?.id)

        val active = dao.observeActiveSession().first()!!.session
        dao.updateSession(active.copy(finishedAt = now.plus(1, ChronoUnit.HOURS)))

        assertNull(dao.observeActiveSession().first())
        assertEquals(2, dao.observeFinishedSessions().first().size)
    }

    @Test
    fun lastCompletedSets_comeFromMostRecentFinishedWorkout() = runTest {
        val exerciseId = db.exerciseDao().insert(exercise("Latzug"))
        val older = finishedSession(now.minus(7, ChronoUnit.DAYS))
        val newer = finishedSession(now.minus(2, ChronoUnit.DAYS))
        val running = dao.insertSession(
            WorkoutSessionEntity(planDayId = null, planDayName = "Einheit A", startedAt = now)
        )
        dao.insertSets(
            listOf(
                set(older, exerciseId, 0, 35_000),
                set(newer, exerciseId, 1, 42_500),
                set(newer, exerciseId, 0, 40_000),
                set(newer, exerciseId, 2, 45_000, completedAt = null),
                set(running, exerciseId, 0, 50_000),
            )
        )

        val last = dao.getLastCompletedSets(exerciseId)

        assertEquals(listOf(40_000, 42_500), last.map { it.actualWeightGrams })
    }

    @Test
    fun deletingPlanDay_keepsHistoryWithCopiedName() = runTest {
        val planId = db.planDao().insertPlan(
            PlanEntity(name = "Plan", sessionsPerWeek = 3, createdAt = now)
        )
        val dayId = db.planDao().insertDay(PlanDayEntity(planId = planId, name = "Einheit A", position = 0))
        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                planDayId = dayId,
                planDayName = "Einheit A",
                startedAt = now,
                finishedAt = now.plus(1, ChronoUnit.HOURS),
            )
        )

        db.planDao().deletePlan(db.planDao().observePlanWithDays(planId).first()!!.plan)

        val session = dao.observeSession(sessionId).first()!!.session
        assertNull(session.planDayId)
        assertEquals("Einheit A", session.planDayName)
    }

    @Test
    fun finishedCountSince_countsOnlyFinishedInRange() = runTest {
        finishedSession(now.minus(10, ChronoUnit.DAYS))
        finishedSession(now.minus(2, ChronoUnit.DAYS))
        finishedSession(now.minus(1, ChronoUnit.DAYS))
        dao.insertSession(WorkoutSessionEntity(planDayId = null, planDayName = "Läuft", startedAt = now))

        assertEquals(2, dao.observeFinishedCountSince(now.minus(7, ChronoUnit.DAYS)).first())
    }
}
