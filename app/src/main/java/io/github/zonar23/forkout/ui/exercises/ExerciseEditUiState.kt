package io.github.zonar23.forkout.ui.exercises

import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup

data class ExerciseEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    val type: ExerciseType = ExerciseType.MACHINE,
    val muscleGroups: Set<MuscleGroup> = emptySet(),
    val setupNote: String = "",
    val isArchived: Boolean = false,
    /** Shown after a save attempt with an empty name. */
    val showNameError: Boolean = false,
    val showRemoveDialog: Boolean = false,
    /** Set once the screen should close (saved, removed or not found). */
    val isDone: Boolean = false,
)
