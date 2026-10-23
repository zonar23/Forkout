package io.github.zonar23.forkout.ui.exercises

import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup

data class ExerciseFilter(
    val query: String = "",
    val type: ExerciseType? = null,
    val muscleGroup: MuscleGroup? = null,
    val showArchived: Boolean = false,
)

data class ExercisesUiState(
    val filter: ExerciseFilter = ExerciseFilter(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val isLoading: Boolean = true,
)

/** Applies [filter] to [exercises]. Search ignores case and surrounding whitespace. */
fun List<ExerciseEntity>.applyFilter(filter: ExerciseFilter): List<ExerciseEntity> {
    val query = filter.query.trim()
    return filter { exercise ->
        (filter.showArchived || !exercise.isArchived) &&
            (query.isEmpty() || exercise.name.contains(query, ignoreCase = true)) &&
            (filter.type == null || exercise.type == filter.type) &&
            (filter.muscleGroup == null || filter.muscleGroup in exercise.muscleGroups)
    }
}
