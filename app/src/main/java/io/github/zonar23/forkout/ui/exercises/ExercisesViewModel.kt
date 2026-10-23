package io.github.zonar23.forkout.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    repository: ExerciseRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(ExerciseFilter())

    val uiState: StateFlow<ExercisesUiState> =
        combine(repository.observeAll(), filter) { exercises, filter ->
            ExercisesUiState(
                filter = filter,
                exercises = exercises.applyFilter(filter),
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }

    /** Selecting the active type again clears the type filter. */
    fun onTypeToggle(type: ExerciseType) =
        filter.update { it.copy(type = if (it.type == type) null else type) }

    fun onMuscleGroupToggle(group: MuscleGroup) =
        filter.update { it.copy(muscleGroup = if (it.muscleGroup == group) null else group) }

    fun onShowArchivedToggle() = filter.update { it.copy(showArchived = !it.showArchived) }
}
