package io.github.zonar23.forkout.ui.workout

import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSessionEntity? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    /** Set shown in the focus area: the user's choice, otherwise the first open set. */
    val focusedSetId: Long? = null,
    /** Rest after this set was skipped and stays hidden. */
    val skippedRestSetId: Long? = null,
    /** Overview cards the user expanded, by [WorkoutExercise.key]. */
    val expandedKeys: Set<Long> = emptySet(),
    /** Value being typed on the number pad. */
    val numberInput: NumberInput? = null,
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
    val focus: Focus? get() = exercises.focus(focusedSetId)

    /** No workout is running (anymore), e.g. it was discarded elsewhere. */
    val isGone: Boolean get() = !isLoading && session == null && !isClosing && finishedSessionId == null
}

enum class NumberField { REPS, WEIGHT }

data class NumberInput(
    val setId: Long,
    val field: NumberField,
    /** Current value as text, e.g. "82,5". */
    val initialText: String,
)
