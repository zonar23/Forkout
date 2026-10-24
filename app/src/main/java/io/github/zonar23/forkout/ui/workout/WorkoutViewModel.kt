package io.github.zonar23.forkout.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.repository.RestStateRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.timer.RestInfo
import io.github.zonar23.forkout.timer.RestTimerScheduler
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Actual values entered in this session that may not have reached the database yet. */
private data class ActualValues(val reps: Int?, val weightGrams: Int?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val restTimer: RestTimerScheduler,
    private val restState: RestStateRepository,
) : ViewModel() {

    private val local = MutableStateFlow(WorkoutUiState(isLoading = false))
    private val active = repository.observeActive()

    /**
     * Latest values per set id, applied on top of the database. Holding ± steps many times per
     * second; computing each step from this overlay instead of the database loses none of them.
     */
    private val actualOverrides = MutableStateFlow<Map<Long, ActualValues>>(emptyMap())

    /** Keeps database writes in the order the user made them. */
    private val writeMutex = Mutex()

    /** Last finished values per exercise id, loaded once per set of exercises. */
    private val lastSets: Flow<Map<Long, List<WorkoutSetEntity>>> = active
        .map { workout -> workout?.sets.orEmpty().map { it.exercise.id }.distinct() }
        .distinctUntilChanged()
        .mapLatest { ids -> ids.associateWith { repository.lastCompletedSets(it) } }

    /** The skip comes from storage, because it can also happen in the notification. */
    private val localWithSkip = combine(local, restState.skippedRestSetId) { local, skipped ->
        local.copy(skippedRestSetId = skipped)
    }

    val uiState: StateFlow<WorkoutUiState> =
        combine(active, lastSets, actualOverrides, localWithSkip) { workout, lastSets, overrides, local ->
            val exercises = workout?.sets.orEmpty().groupByExercise().map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        overrides[set.id]?.let { set.copy(actualReps = it.reps, actualWeightGrams = it.weightGrams) } ?: set
                    },
                    lastSets = lastSets[exercise.exercise.id].orEmpty(),
                )
            }
            val openIds = exercises.flatMap { it.sets }.filter { it.completedAt == null }.map { it.id }
            local.copy(
                isLoading = false,
                session = workout?.session,
                exercises = exercises,
                // Auto-advance: once the focused set is done, the next open set takes over.
                focusedSetId = local.focusedSetId?.takeIf { it in openIds } ?: exercises.firstOpenSet()?.id,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun focus(setId: Long) = local.update { it.copy(focusedSetId = setId) }

    fun toggleExpanded(exerciseKey: Long) = local.update {
        val keys = if (exerciseKey in it.expandedKeys) it.expandedKeys - exerciseKey else it.expandedKeys + exerciseKey
        it.copy(expandedKeys = keys)
    }

    fun stepReps(setId: Long, delta: Int) = updateActual(setId) { reps, weight ->
        ((reps ?: 0) + delta).coerceIn(0, MAX_REPS) to weight
    }

    fun stepWeight(setId: Long, deltaGrams: Int) = updateActual(setId) { reps, weight ->
        reps to ((weight ?: 0) + deltaGrams).coerceIn(0, MAX_WEIGHT_GRAMS)
    }

    /** Copies reps and weight of the matching set from the last workout. */
    fun applyLastTime(setId: Long) {
        val exercise = uiState.value.exercises.firstOrNull { exercise -> exercise.sets.any { it.id == setId } } ?: return
        val last = exercise.lastTimeFor(exercise.sets.first { it.id == setId }) ?: return
        updateActual(setId) { _, _ -> last.actualReps to last.actualWeightGrams }
    }

    fun openNumberInput(set: WorkoutSetEntity, field: NumberField) = local.update {
        val text = when (field) {
            NumberField.REPS -> (set.actualReps ?: 0).toString()
            NumberField.WEIGHT -> WeightFormat.formatKg(set.actualWeightGrams ?: 0)
        }
        it.copy(numberInput = NumberInput(set.id, field, text))
    }

    fun dismissNumberInput() = local.update { it.copy(numberInput = null) }

    /** @return false if [text] is not a valid value for the open field. */
    fun isValidNumber(text: String): Boolean = parseNumber(text) != null

    fun confirmNumberInput(text: String) {
        val input = local.value.numberInput ?: return
        val value = parseNumber(text) ?: return
        updateActual(input.setId) { reps, weight ->
            when (input.field) {
                NumberField.REPS -> value to weight
                NumberField.WEIGHT -> reps to value
            }
        }
        dismissNumberInput()
    }

    /** Checks or unchecks a set. Checking starts its rest timer, even if an older one was skipped. */
    fun toggleComplete(set: WorkoutSetEntity) {
        val complete = set.completedAt == null
        // The next open set in workout order, as it will be once this one is done.
        val next = uiState.value.exercises
            .flatMap { exercise -> exercise.sets.mapIndexed { index, it -> Triple(exercise, index, it) } }
            .firstOrNull { (_, _, it) -> it.completedAt == null && it.id != set.id }
        viewModelScope.launch {
            // Pending value changes must be stored before the set is completed.
            val stored = writeMutex.withLock { repository.setCompleted(set.id, complete) }
            if (complete) {
                local.update { it.copy(focusedSetId = null) }
                restState.clear()
                val completedAt = stored?.completedAt
                if (completedAt != null && set.restSeconds > 0) {
                    restTimer.start(
                        RestInfo(
                            afterSetId = set.id,
                            endsAt = completedAt.plusSeconds(set.restSeconds.toLong()),
                            totalSeconds = set.restSeconds,
                            nextExerciseName = next?.first?.exercise?.name,
                            nextSetNumber = (next?.second ?: 0) + 1,
                        )
                    )
                }
            } else {
                restTimer.cancel()
            }
        }
    }

    fun skipRest(afterSetId: Long) {
        restTimer.cancel()
        viewModelScope.launch { restState.skipRestAfter(afterSetId) }
    }

    /** Postpones an exercise, e.g. because its machine is taken. */
    fun moveToEnd(exercise: WorkoutExercise) {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            writeMutex.withLock { repository.moveExerciseToEnd(session.id, exercise.position) }
            // Focus the next open set in the new order, unless the user picked one elsewhere.
            local.update { state ->
                if (exercise.sets.any { it.id == state.focusedSetId }) state.copy(focusedSetId = null) else state
            }
        }
    }

    /** Finishes directly when all sets are done, otherwise asks first. */
    fun requestFinish() {
        if (uiState.value.openSets > 0) local.update { it.copy(showFinishDialog = true) } else finish()
    }

    fun dismissFinish() = local.update { it.copy(showFinishDialog = false) }

    fun finish() {
        val session = uiState.value.session ?: return
        local.update { it.copy(showFinishDialog = false, isClosing = true) }
        viewModelScope.launch {
            writeMutex.withLock { repository.finish(session.id) }
            endRest()
            local.update { it.copy(finishedSessionId = session.id) }
        }
    }

    fun requestDiscard() = local.update { it.copy(showDiscardDialog = true) }

    fun dismissDiscard() = local.update { it.copy(showDiscardDialog = false) }

    fun confirmDiscard() {
        val session = uiState.value.session ?: return
        local.update { it.copy(showDiscardDialog = false, isClosing = true) }
        viewModelScope.launch {
            writeMutex.withLock { repository.discard(session.id) }
            endRest()
            local.update { it.copy(isClosing = false) }
        }
    }

    /** No rest notification or alarm may outlive the workout. */
    private suspend fun endRest() {
        restTimer.cancel()
        restState.clear()
    }

    private fun parseNumber(text: String): Int? = when (local.value.numberInput?.field) {
        NumberField.REPS -> text.toIntOrNull()?.takeIf { it in 0..MAX_REPS }
        NumberField.WEIGHT -> WeightFormat.parseKg(text)
        null -> null
    }

    /** Applies [change] to the newest values of [setId] immediately and stores them in order. */
    private fun updateActual(setId: Long, change: (reps: Int?, weightGrams: Int?) -> Pair<Int?, Int?>) {
        val set = uiState.value.allSets.firstOrNull { it.id == setId } ?: return
        var stored: ActualValues? = null
        actualOverrides.update { overrides ->
            val current = overrides[setId] ?: ActualValues(set.actualReps, set.actualWeightGrams)
            val (reps, weight) = change(current.reps, current.weightGrams)
            ActualValues(reps, weight).also { stored = it }.let { overrides + (setId to it) }
        }
        val values = stored ?: return
        viewModelScope.launch {
            writeMutex.withLock { repository.updateActual(setId, values.reps, values.weightGrams, set.rir) }
        }
    }

    companion object {
        const val MAX_REPS = 999
        const val MAX_WEIGHT_GRAMS = 1_000_000
        const val REPS_STEP = 1
        const val WEIGHT_STEP_GRAMS = 2_500
    }
}
