package io.github.zonar23.forkout.ui.plans

import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanEntity

data class PlanDayItem(
    val day: PlanDayEntity,
    val exerciseCount: Int,
)

data class PlanDetailUiState(
    val isLoading: Boolean = true,
    val plan: PlanEntity? = null,
    val days: List<PlanDayItem> = emptyList(),
    /** Day whose rename dialog is open, with the edited name. */
    val renaming: PlanDayEntity? = null,
    val renameText: String = "",
    /** Day whose delete confirmation is open. */
    val deleting: PlanDayEntity? = null,
    /** Id of a freshly copied plan the screen should open. */
    val copiedPlanId: Long? = null,
) {
    /** True once loading finished and the plan does not exist (anymore). */
    val isGone: Boolean get() = !isLoading && plan == null
}

/** Letter for the next default day name: A, B, …, Z, then numbers. */
fun nextDayLabel(existingDays: Int): String =
    if (existingDays < 26) ('A' + existingDays).toString() else (existingDays + 1).toString()
