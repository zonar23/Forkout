package io.github.zonar23.forkout.ui.today

import io.github.zonar23.forkout.data.db.relation.SessionOverview

data class TodayUiState(
    val isLoading: Boolean = true,
    val nextUp: NextUp? = null,
    val nextExerciseCount: Int = 0,
    /** Finished workouts since Monday. */
    val weekDone: Int = 0,
    /** Target from the suggested plan, or null without a plan. */
    val weekTarget: Int? = null,
    val lastWorkout: SessionOverview? = null,
    /** Name of a running workout; the start button then resumes it. */
    val activeWorkoutName: String? = null,
    /** Set once the workout screen should open. */
    val openWorkout: Boolean = false,
)
