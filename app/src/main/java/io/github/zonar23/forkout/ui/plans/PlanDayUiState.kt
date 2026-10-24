package io.github.zonar23.forkout.ui.plans

import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.relation.PlanExerciseWithDetails
import io.github.zonar23.forkout.ui.components.WeightFormat

data class PlanDayUiState(
    val isLoading: Boolean = true,
    val day: PlanDayEntity? = null,
    val exercises: List<PlanExerciseWithDetails> = emptyList(),
    val setEditor: SetEditorState? = null,
    /** Exercise whose remove confirmation is open. */
    val removing: PlanExerciseEntity? = null,
    /** Name of the unfinished workout that blocks starting a new one; shows a dialog. */
    val blockingWorkoutName: String? = null,
    /** Set once the workout screen should open (started or resumed). */
    val openWorkout: Boolean = false,
    /** Plan exercises shown with their sets; the others show a one-line summary. */
    val expandedIds: Set<Long> = emptySet(),
) {
    val isGone: Boolean get() = !isLoading && day == null
}

/** Compact values of a collapsed exercise, e.g. reps "10–12" and weight "75–80". */
data class SetsSummary(
    val count: Int,
    val reps: String,
    val weightKg: String,
)

fun List<PlanSetEntity>.summarize(): SetsSummary {
    fun range(values: List<Int>, format: (Int) -> String): String {
        val min = values.minOrNull() ?: return format(0)
        val max = values.max()
        return if (min == max) format(min) else "${format(min)}–${format(max)}"
    }
    return SetsSummary(
        count = size,
        reps = range(map { it.targetReps }) { it.toString() },
        weightKg = range(map { it.targetWeightGrams }) { WeightFormat.formatKg(it) },
    )
}

/** Text-based state of the set dialog so that partial input like "82," can be typed. */
data class SetEditorState(
    val set: PlanSetEntity,
    /** 1-based number shown in the title. */
    val setNumber: Int,
    val setCount: Int,
    val repsText: String,
    val weightText: String,
    val restText: String,
    val applyToAll: Boolean = false,
) {
    val reps: Int? get() = repsText.trim().toIntOrNull()?.takeIf { it in 0..MAX_REPS }
    val weightGrams: Int? get() = WeightFormat.parseKg(weightText)
    val restSeconds: Int? get() = restText.trim().toIntOrNull()?.takeIf { it in 0..MAX_REST_SECONDS }
    val isValid: Boolean get() = reps != null && weightGrams != null && restSeconds != null

    companion object {
        const val MAX_REPS = 999
        const val MAX_REST_SECONDS = 3600
        const val REPS_STEP = 1
        const val WEIGHT_STEP_GRAMS = 2_500
        const val REST_STEP_SECONDS = 15

        fun from(set: PlanSetEntity, setNumber: Int, setCount: Int) = SetEditorState(
            set = set,
            setNumber = setNumber,
            setCount = setCount,
            repsText = set.targetReps.toString(),
            weightText = WeightFormat.formatKg(set.targetWeightGrams),
            restText = set.restSeconds.toString(),
        )
    }
}
