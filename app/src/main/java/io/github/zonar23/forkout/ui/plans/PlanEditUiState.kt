package io.github.zonar23.forkout.ui.plans

data class PlanEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    val goal: String = "",
    val sessionsPerWeek: Int = DEFAULT_SESSIONS_PER_WEEK,
    val note: String = "",
    val showNameError: Boolean = false,
    val showDeleteDialog: Boolean = false,
    /** Set once the screen should close; [result] tells the caller where to go next. */
    val result: PlanEditResult? = null,
) {
    companion object {
        const val DEFAULT_SESSIONS_PER_WEEK = 3
        const val MIN_SESSIONS_PER_WEEK = 1
        const val MAX_SESSIONS_PER_WEEK = 7
    }
}

sealed interface PlanEditResult {
    data class Created(val planId: Long) : PlanEditResult
    data object Saved : PlanEditResult
    data object Deleted : PlanEditResult
    data object NotFound : PlanEditResult
}
