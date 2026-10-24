package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.SettingsRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.navigation.PlanDayRoute
import io.github.zonar23.forkout.ui.plans.SetEditorState.Companion.MAX_REPS
import io.github.zonar23.forkout.ui.plans.SetEditorState.Companion.MAX_REST_SECONDS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanDayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val dayId = savedStateHandle.toRoute<PlanDayRoute>().dayId

    private val local = MutableStateFlow(PlanDayUiState(isLoading = false))

    val uiState: StateFlow<PlanDayUiState> = combine(repository.observeDay(dayId), local) { day, local ->
        local.copy(isLoading = false, day = day?.day, exercises = day?.exercises.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanDayUiState())

    init {
        // Newly added exercises open automatically so their sets can be adjusted right away.
        viewModelScope.launch {
            var knownIds: Set<Long>? = null
            repository.observeDay(dayId).collect { day ->
                val ids = day?.exercises.orEmpty().map { it.planExercise.id }.toSet()
                val added = knownIds?.let { ids - it }.orEmpty()
                if (added.isNotEmpty()) local.update { it.copy(expandedIds = it.expandedIds + added) }
                knownIds = ids
            }
        }
    }

    fun toggleExpanded(planExerciseId: Long) = local.update {
        val expanded = if (planExerciseId in it.expandedIds) {
            it.expandedIds - planExerciseId
        } else {
            it.expandedIds + planExerciseId
        }
        it.copy(expandedIds = expanded)
    }

    fun onReorder(planExercises: List<PlanExerciseEntity>) {
        viewModelScope.launch { repository.reorderExercises(planExercises) }
    }

    /** Appends a copy of the last set; the first set uses the default rest from the settings. */
    fun addSet(planExerciseId: Long) {
        viewModelScope.launch {
            val fallback = PlanSetEntity(
                planExerciseId = planExerciseId,
                position = 0,
                targetReps = DEFAULT_REPS,
                targetWeightGrams = 0,
                restSeconds = settingsRepository.settings.first().defaultRestSeconds,
            )
            repository.addSet(planExerciseId, fallback)
        }
    }

    // Set dialog

    fun openSet(set: PlanSetEntity, setNumber: Int, setCount: Int) =
        local.update { it.copy(setEditor = SetEditorState.from(set, setNumber, setCount)) }

    fun dismissSet() = local.update { it.copy(setEditor = null) }

    fun onRepsChange(text: String) = updateEditor { it.copy(repsText = text) }

    fun onWeightChange(text: String) = updateEditor { it.copy(weightText = text) }

    fun onRestChange(text: String) = updateEditor { it.copy(restText = text) }

    fun onApplyToAllChange(applyToAll: Boolean) = updateEditor { it.copy(applyToAll = applyToAll) }

    fun stepReps(delta: Int) = updateEditor {
        it.copy(repsText = ((it.reps ?: 0) + delta).coerceIn(0, MAX_REPS).toString())
    }

    fun stepWeight(deltaGrams: Int) = updateEditor {
        it.copy(weightText = WeightFormat.formatKg(((it.weightGrams ?: 0) + deltaGrams).coerceAtLeast(0)))
    }

    fun stepRest(delta: Int) = updateEditor {
        it.copy(restText = ((it.restSeconds ?: 0) + delta).coerceIn(0, MAX_REST_SECONDS).toString())
    }

    fun saveSet() {
        val editor = local.value.setEditor ?: return
        val reps = editor.reps ?: return
        val weight = editor.weightGrams ?: return
        val rest = editor.restSeconds ?: return
        viewModelScope.launch {
            if (editor.applyToAll) {
                repository.applyToAllSets(editor.set.planExerciseId, reps, weight, rest)
            } else {
                repository.updateSet(editor.set.copy(targetReps = reps, targetWeightGrams = weight, restSeconds = rest))
            }
            dismissSet()
        }
    }

    fun deleteSet() {
        val editor = local.value.setEditor ?: return
        viewModelScope.launch {
            repository.deleteSet(editor.set)
            dismissSet()
        }
    }

    // Workout

    /** Starts a workout for this day, unless another one is still running. */
    fun startWorkout() {
        viewModelScope.launch {
            val active = workoutRepository.observeActive().first()
            if (active != null) {
                local.update { it.copy(blockingWorkoutName = active.session.planDayName) }
            } else {
                workoutRepository.start(dayId)
                local.update { it.copy(openWorkout = true) }
            }
        }
    }

    fun resumeBlockingWorkout() = local.update { it.copy(blockingWorkoutName = null, openWorkout = true) }

    fun dismissBlockingWorkout() = local.update { it.copy(blockingWorkoutName = null) }

    fun onWorkoutOpened() = local.update { it.copy(openWorkout = false) }

    // Removing exercises

    fun startRemove(planExercise: PlanExerciseEntity) = local.update { it.copy(removing = planExercise) }

    fun dismissRemove() = local.update { it.copy(removing = null) }

    fun confirmRemove() {
        val planExercise = local.value.removing ?: return
        viewModelScope.launch {
            repository.removeExercise(planExercise)
            dismissRemove()
        }
    }

    private fun updateEditor(transform: (SetEditorState) -> SetEditorState) =
        local.update { state -> state.copy(setEditor = state.setEditor?.let(transform)) }

    companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 10
    }
}
