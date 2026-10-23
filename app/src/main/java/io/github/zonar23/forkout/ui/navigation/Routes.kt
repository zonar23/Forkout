package io.github.zonar23.forkout.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object PlansRoute

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
