package io.github.zonar23.forkout.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.ExerciseHistoryEntry
import io.github.zonar23.forkout.data.repository.ExerciseRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.navigation.ExerciseEditRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val exerciseId = savedStateHandle.toRoute<ExerciseEditRoute>().exerciseId
    private val isNew = exerciseId == ExerciseEditRoute.NEW_ID

    /** Most recent workouts with this exercise, newest first. */
    val history: StateFlow<List<ExerciseHistoryEntry>> =
        (if (isNew) flowOf(emptyList()) else workoutRepository.observeExerciseHistory(exerciseId))
            .map { it.take(HISTORY_LIMIT) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The stored exercise, kept so that fields not shown in the editor survive an update. */
    private var original: ExerciseEntity? = null

    private val _uiState = MutableStateFlow(ExerciseEditUiState(isNew = isNew, isLoading = !isNew))
    val uiState: StateFlow<ExerciseEditUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                val exercise = repository.get(exerciseId)
                original = exercise
                _uiState.update { state ->
                    if (exercise == null) {
                        state.copy(isLoading = false, isDone = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            name = exercise.name,
                            type = exercise.type,
                            muscleGroups = exercise.muscleGroups,
                            setupNote = exercise.setupNote,
                            isArchived = exercise.isArchived,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, showNameError = false) }

    fun onTypeSelect(type: ExerciseType) = _uiState.update { it.copy(type = type) }

    fun onMuscleGroupToggle(group: MuscleGroup) = _uiState.update {
        it.copy(muscleGroups = if (group in it.muscleGroups) it.muscleGroups - group else it.muscleGroups + group)
    }

    fun onSetupNoteChange(note: String) = _uiState.update { it.copy(setupNote = note) }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            val base = original
            if (base == null) {
                repository.create(
                    ExerciseEntity(
                        name = name,
                        type = state.type,
                        muscleGroups = state.muscleGroups,
                        setupNote = state.setupNote.trim(),
                    )
                )
            } else {
                repository.update(
                    base.copy(
                        name = name,
                        type = state.type,
                        muscleGroups = state.muscleGroups,
                        setupNote = state.setupNote.trim(),
                    )
                )
            }
            _uiState.update { it.copy(isDone = true) }
        }
    }

    fun onRemoveClick() = _uiState.update { it.copy(showRemoveDialog = true) }

    fun onRemoveDismiss() = _uiState.update { it.copy(showRemoveDialog = false) }

    fun confirmRemove() {
        if (isNew) return
        viewModelScope.launch {
            repository.remove(exerciseId)
            _uiState.update { it.copy(showRemoveDialog = false, isDone = true) }
        }
    }

    fun restore() {
        if (isNew) return
        viewModelScope.launch {
            repository.restore(exerciseId)
            original = original?.copy(isArchived = false)
            _uiState.update { it.copy(isArchived = false) }
        }
    }

    companion object {
        const val HISTORY_LIMIT = 10
    }
}
