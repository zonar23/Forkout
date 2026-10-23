package io.github.zonar23.forkout.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.WorkoutSessionEntity
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.navigation.HistoryDetailRoute
import io.github.zonar23.forkout.ui.workout.WorkoutExercise
import io.github.zonar23.forkout.ui.workout.WorkoutSummary
import io.github.zonar23.forkout.ui.workout.groupByExercise
import io.github.zonar23.forkout.ui.workout.summarize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val session: WorkoutSessionEntity? = null,
    val summary: WorkoutSummary? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val showDeleteDialog: Boolean = false,
) {
    /** The workout no longer exists, e.g. after deleting it. */
    val isGone: Boolean get() = !isLoading && session == null
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val sessionId = savedStateHandle.toRoute<HistoryDetailRoute>().sessionId
    private val local = MutableStateFlow(HistoryDetailUiState(isLoading = false))

    val uiState: StateFlow<HistoryDetailUiState> =
        combine(repository.observeSession(sessionId), local) { workout, local ->
            val exercises = workout?.sets.orEmpty().groupByExercise()
            val finishedAt = workout?.session?.finishedAt
            local.copy(
                isLoading = false,
                session = workout?.session,
                exercises = exercises,
                summary = if (workout != null && finishedAt != null) {
                    summarize(workout.session.startedAt, finishedAt, exercises)
                } else {
                    null
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailUiState())

    fun onDeleteClick() = local.update { it.copy(showDeleteDialog = true) }

    fun onDeleteDismiss() = local.update { it.copy(showDeleteDialog = false) }

    /** Removes the workout; the screen closes once it is gone. */
    fun confirmDelete() {
        local.update { it.copy(showDeleteDialog = false) }
        viewModelScope.launch { repository.discard(sessionId) }
    }
}
