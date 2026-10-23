package io.github.zonar23.forkout.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import io.github.zonar23.forkout.ui.navigation.WorkoutSummaryRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val dayName: String = "",
    val summary: WorkoutSummary? = null,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: WorkoutRepository,
) : ViewModel() {

    private val sessionId = savedStateHandle.toRoute<WorkoutSummaryRoute>().sessionId

    val uiState: StateFlow<WorkoutSummaryUiState> = repository.observeSession(sessionId)
        .map { workout ->
            val finishedAt = workout?.session?.finishedAt
            WorkoutSummaryUiState(
                isLoading = false,
                dayName = workout?.session?.planDayName.orEmpty(),
                summary = if (workout != null && finishedAt != null) {
                    summarize(workout.session.startedAt, finishedAt, workout.sets.groupByExercise())
                } else {
                    null
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutSummaryUiState())
}
