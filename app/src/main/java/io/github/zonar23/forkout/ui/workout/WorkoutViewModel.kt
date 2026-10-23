package io.github.zonar23.forkout.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.components.WeightFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val local = MutableStateFlow(WorkoutUiState(isLoading = false))
    private val active = repository.observeActive()

    /** Last finished values per exercise id, loaded once per set of exercises. */
    private val lastSets: Flow<Map<Long, List<WorkoutSetEntity>>> = active
        .map { workout -> workout?.sets.orEmpty().map { it.exercise.id }.distinct() }
        .distinctUntilChanged()
        .mapLatest { ids -> ids.associateWith { repository.lastCompletedSets(it) } }

    val uiState: StateFlow<WorkoutUiState> = combine(active, lastSets, local) { workout, lastSets, local ->
        val exercises = workout?.sets.orEmpty().groupByExercise().map { exercise ->
            exercise.copy(lastSets = lastSets[exercise.exercise.id].orEmpty())
        }
        val openIds = exercises.flatMap { it.sets }.filter { it.completedAt == null }.map { it.id }
        local.copy(
            isLoading = false,
            session = workout?.session,
            exercises = exercises,
            focusedSetId = local.focusedSetId?.takeIf { it in openIds } ?: exercises.firstOpenSet()?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun focus(setId: Long) = local.update { it.copy(focusedSetId = setId) }

    fun stepReps(set: WorkoutSetEntity, delta: Int) {
        val reps = ((set.actualReps ?: set.targetReps ?: 0) + delta).coerceIn(0, MAX_REPS)
        viewModelScope.launch { repository.updateActual(set.id, reps, set.actualWeightGrams, set.rir) }
    }

    fun stepWeight(set: WorkoutSetEntity, deltaGrams: Int) {
        val weight = ((set.actualWeightGrams ?: set.targetWeightGrams ?: 0) + deltaGrams).coerceAtLeast(0)
        viewModelScope.launch { repository.updateActual(set.id, set.actualReps, weight, set.rir) }
    }

    fun openWeightInput(set: WorkoutSetEntity) = local.update {
        it.copy(weightInput = WeightInput(set.id, WeightFormat.formatKg(set.actualWeightGrams ?: 0)))
    }

    fun onWeightInputChange(text: String) = local.update { state ->
        state.copy(weightInput = state.weightInput?.copy(text = text))
    }

    fun dismissWeightInput() = local.update { it.copy(weightInput = null) }

    fun confirmWeightInput() {
        val input = local.value.weightInput ?: return
        val grams = WeightFormat.parseKg(input.text) ?: return
        val set = uiState.value.allSets.firstOrNull { it.id == input.setId } ?: return
        viewModelScope.launch {
            repository.updateActual(set.id, set.actualReps, grams, set.rir)
            dismissWeightInput()
        }
    }

    /** Checks or unchecks a set. Checking starts its rest timer, even if an older one was skipped. */
    fun toggleComplete(set: WorkoutSetEntity) {
        val complete = set.completedAt == null
        viewModelScope.launch {
            repository.setCompleted(set.id, complete)
            if (complete) local.update { it.copy(focusedSetId = null, skippedRestSetId = null) }
        }
    }

    fun skipRest(afterSetId: Long) = local.update { it.copy(skippedRestSetId = afterSetId) }

    /** Finishes directly when all sets are done, otherwise asks first. */
    fun requestFinish() {
        if (uiState.value.openSets > 0) local.update { it.copy(showFinishDialog = true) } else finish()
    }

    fun dismissFinish() = local.update { it.copy(showFinishDialog = false) }

    fun finish() {
        val session = uiState.value.session ?: return
        local.update { it.copy(showFinishDialog = false, isClosing = true) }
        viewModelScope.launch {
            repository.finish(session.id)
            local.update { it.copy(finishedSessionId = session.id) }
        }
    }

    fun requestDiscard() = local.update { it.copy(showDiscardDialog = true) }

    fun dismissDiscard() = local.update { it.copy(showDiscardDialog = false) }

    fun confirmDiscard() {
        val session = uiState.value.session ?: return
        local.update { it.copy(showDiscardDialog = false, isClosing = true) }
        viewModelScope.launch {
            repository.discard(session.id)
            local.update { it.copy(isClosing = false) }
        }
    }

    companion object {
        const val MAX_REPS = 999
        const val REPS_STEP = 1
        const val WEIGHT_STEP_GRAMS = 2_500
    }
}
