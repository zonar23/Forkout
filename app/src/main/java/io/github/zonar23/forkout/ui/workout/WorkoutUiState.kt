package io.github.zonar23.forkout.ui.workout

import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSessionEntity? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    /** Set whose steppers are expanded: the user's choice, otherwise the first open set. */
    val focusedSetId: Long? = null,
    /** Rest after this set was skipped and stays hidden. */
    val skippedRestSetId: Long? = null,
    /** Set whose weight is being typed in a dialog, with the current text. */
    val weightInput: WeightInput? = null,
    val showFinishDialog: Boolean = false,
    val showDiscardDialog: Boolean = false,
    /** Set after finishing; the screen then opens the summary for this session. */
    val finishedSessionId: Long? = null,
    /** True while finishing or discarding, so the screen does not close too early. */
    val isClosing: Boolean = false,
) {
    val allSets: List<WorkoutSetEntity> get() = exercises.flatMap { it.sets }
    val completedSets: Int get() = allSets.count { it.completedAt != null }
    val openSets: Int get() = allSets.size - completedSets

    /** No workout is running (anymore), e.g. it was discarded elsewhere. */
    val isGone: Boolean get() = !isLoading && session == null && !isClosing && finishedSessionId == null
}

data class WeightInput(
    val setId: Long,
    val text: String,
)
