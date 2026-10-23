package io.github.zonar23.forkout.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(TodayUiState(isLoading = false))

    private val nextUp = combine(
        planRepository.observePlansWithDays(),
        workoutRepository.observeLastFinished().mapLatest { session ->
            session?.planDayId?.let { planRepository.getDay(it) }
        },
    ) { plans, lastDay -> nextUp(plans, lastDay) }

    private val nextExerciseCount = nextUp.flatMapLatest { next ->
        if (next == null) {
            flowOf(0)
        } else {
            planRepository.observeExerciseCounts(next.plan.id).map { it[next.day.id] ?: 0 }
        }
    }

    // The week start is fixed when the screen opens; a new week shows after reopening.
    private val weekDone = workoutRepository.observeFinishedCountSince(startOfWeek(clock.instant(), clock.zone))

    val uiState: StateFlow<TodayUiState> = combine(
        nextUp,
        nextExerciseCount,
        weekDone,
        workoutRepository.observeOverviews().map { it.firstOrNull() },
        combine(workoutRepository.observeActive(), local) { active, local -> active to local },
    ) { next, count, done, last, (active, local) ->
        local.copy(
            isLoading = false,
            nextUp = next,
            nextExerciseCount = count,
            weekDone = done,
            weekTarget = next?.plan?.sessionsPerWeek,
            lastWorkout = last,
            activeWorkoutName = active?.session?.planDayName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Resumes the running workout or starts the suggested day. */
    fun startOrResume() {
        viewModelScope.launch {
            if (workoutRepository.observeActive().first() == null) {
                val day = uiState.value.nextUp?.day ?: return@launch
                workoutRepository.start(day.id)
            }
            local.update { it.copy(openWorkout = true) }
        }
    }

    fun onWorkoutOpened() = local.update { it.copy(openWorkout = false) }
}
