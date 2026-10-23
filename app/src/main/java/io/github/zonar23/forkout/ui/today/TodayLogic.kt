package io.github.zonar23.forkout.ui.today

import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.db.relation.PlanWithDays
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class NextUp(
    val plan: PlanEntity,
    val day: PlanDayEntity,
)

/**
 * Suggests the day to train next.
 *
 * If the last workout belongs to one of [plans], the following day of that plan comes next,
 * starting over after the last one. Otherwise the first day of the newest plan with days.
 *
 * @param plans active plans, newest first, days sorted by position
 * @param lastDay day of the most recent finished workout, if it still exists
 */
fun nextUp(plans: List<PlanWithDays>, lastDay: PlanDayEntity?): NextUp? {
    if (lastDay != null) {
        val plan = plans.firstOrNull { it.plan.id == lastDay.planId }
        if (plan != null && plan.days.isNotEmpty()) {
            val index = plan.days.indexOfFirst { it.id == lastDay.id }
            val next = plan.days[(index + 1).mod(plan.days.size)]
            return NextUp(plan.plan, next)
        }
    }
    val newest = plans.firstOrNull { it.days.isNotEmpty() } ?: return null
    return NextUp(newest.plan, newest.days.first())
}

/** Monday 00:00 of the week containing [now], in [zone]. */
fun startOfWeek(now: Instant, zone: ZoneId): Instant =
    now.atZone(zone)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone)
        .toInstant()
