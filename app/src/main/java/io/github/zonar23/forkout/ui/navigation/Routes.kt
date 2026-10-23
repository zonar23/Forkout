package io.github.zonar23.forkout.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object PlansRoute

/** Creates a new plan when [planId] is [NEW_ID], otherwise edits the existing one. */
@Serializable
data class PlanEditRoute(val planId: Long = NEW_ID) {
    companion object {
        const val NEW_ID = 0L
    }
}

@Serializable
data class PlanDetailRoute(val planId: Long)

@Serializable
data class PlanDayRoute(val dayId: Long)

/** Picks an exercise from the library and appends it to the plan day [dayId]. */
@Serializable
data class ExercisePickerRoute(val dayId: Long)

/** The running workout; there is at most one at a time. */
@Serializable
data object WorkoutRoute

@Serializable
data class WorkoutSummaryRoute(val sessionId: Long)

@Serializable
data object ExercisesRoute

/** Creates a new exercise when [exerciseId] is [NEW_ID], otherwise edits the existing one. */
@Serializable
data class ExerciseEditRoute(val exerciseId: Long = NEW_ID) {
    companion object {
        const val NEW_ID = 0L
    }
}

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute
