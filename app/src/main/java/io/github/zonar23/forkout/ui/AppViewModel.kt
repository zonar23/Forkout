package io.github.zonar23.forkout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State of the app shell that does not belong to a single screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    /** Name of the running workout's day, or null if none is running. */
    val activeWorkoutName: StateFlow<String?> = workoutRepository.observeActive()
        .map { it?.session?.planDayName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
