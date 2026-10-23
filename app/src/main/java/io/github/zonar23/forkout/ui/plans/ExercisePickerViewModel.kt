package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.repository.ExerciseRepository
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.ui.exercises.ExerciseFilter
import io.github.zonar23.forkout.ui.exercises.applyFilter
import io.github.zonar23.forkout.ui.navigation.ExercisePickerRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisePickerUiState(
    val query: String = "",
    val exercises: List<ExerciseEntity> = emptyList(),
    val isLoading: Boolean = true,
    /** Set once an exercise was added and the picker should close. */
    val isDone: Boolean = false,
)

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val dayId = savedStateHandle.toRoute<ExercisePickerRoute>().dayId
    private val local = MutableStateFlow(ExercisePickerUiState(isLoading = false))
    private var isAdding = false

    val uiState: StateFlow<ExercisePickerUiState> =
        combine(exerciseRepository.observeActive(), local) { exercises, local ->
            local.copy(isLoading = false, exercises = exercises.applyFilter(ExerciseFilter(query = local.query)))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun onQueryChange(query: String) = local.update { it.copy(query = query) }

    /** Adds the exercise with default sets; values are adjusted afterwards in the day editor. */
    fun pick(exerciseId: Long) {
        // Ignores double taps. isDone is only set after saving, because closing the screen
        // clears this ViewModel and would cancel the insert.
        if (isAdding) return
        isAdding = true
        viewModelScope.launch {
            planRepository.addExercise(
                dayId = dayId,
                exerciseId = exerciseId,
                setCount = PlanDayViewModel.DEFAULT_SETS,
                targetReps = PlanDayViewModel.DEFAULT_REPS,
                targetWeightGrams = 0,
                restSeconds = settingsRepository.settings.first().defaultRestSeconds,
            )
            local.update { it.copy(isDone = true) }
        }
    }
}
