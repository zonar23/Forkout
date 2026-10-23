package io.github.zonar23.forkout.ui.today

import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.relation.PlanWithDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TodayLogicTest {

    private fun plan(id: Long, vararg dayNames: String) = PlanWithDays(
        plan = PlanEntity(id = id, name = "Plan $id", sessionsPerWeek = 3, createdAt = Instant.EPOCH),
        days = dayNames.mapIndexed { index, name ->
            PlanDayEntity(id = id * 10 + index, planId = id, name = name, position = index)
        },
    )

    private val newer = plan(2, "A", "B", "C")
    private val older = plan(1, "X", "Y")

    @Test
    fun withoutHistory_suggestsFirstDayOfNewestPlan() {
        assertEquals("A", nextUp(listOf(newer, older), lastDay = null)?.day?.name)
    }

    @Test
    fun afterAWorkout_suggestsFollowingDayOfSamePlan() {
        val next = nextUp(listOf(newer, older), lastDay = older.days[0])

        assertEquals("Plan 1", next?.plan?.name)
        assertEquals("Y", next?.day?.name)
    }

    @Test
    fun afterLastDay_startsOver() {
        assertEquals("A", nextUp(listOf(newer, older), lastDay = newer.days[2])?.day?.name)
    }

    @Test
    fun lastDayOfArchivedOrDeletedPlan_fallsBackToNewestPlan() {
        val foreignDay = PlanDayEntity(id = 99, planId = 7, name = "Alt", position = 0)

        assertEquals("A", nextUp(listOf(newer), lastDay = foreignDay)?.day?.name)
    }

    @Test
    fun plansWithoutDays_areSkipped() {
        val empty = plan(3)

        assertEquals("Plan 2", nextUp(listOf(empty, newer), lastDay = null)?.plan?.name)
        assertNull(nextUp(listOf(empty), lastDay = null))
    }

    @Test
    fun startOfWeek_isMondayMidnightInLocalZone() {
        val berlin = ZoneId.of("Europe/Berlin")

        // Friday, 23 Oct 2026, 10:00 UTC → Monday, 19 Oct 2026, 00:00 Berlin (CEST, UTC+2)
        assertEquals(
            Instant.parse("2026-10-18T22:00:00Z"),
            startOfWeek(Instant.parse("2026-10-23T10:00:00Z"), berlin),
        )
        // Sunday 23:30 Berlin still belongs to the same week.
        assertEquals(
            Instant.parse("2026-10-18T22:00:00Z"),
            startOfWeek(Instant.parse("2026-10-25T22:30:00Z"), berlin),
        )
    }
}
