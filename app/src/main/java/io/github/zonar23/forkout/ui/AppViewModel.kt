package io.github.zonar23.forkout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.data.repository.RestStateRepository
import io.github.zonar23.forkout.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the mini player shows for a running workout. */
data class MiniPlayerState(
    val dayName: String,
    val completedSets: Int,
    val totalSets: Int,
    /** All sets, so the player can count down the current rest by itself. */
    val sets: List<WorkoutSetEntity>,
    val skippedRestSetId: Long?,
)

/** State of the app shell that does not belong to a single screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    restState: RestStateRepository,
) : ViewModel() {

    /** The running workout, or null if none is running. */
    val miniPlayer: StateFlow<MiniPlayerState?> =
        combine(workoutRepository.observeActive(), restState.skippedRestSetId) { workout, skipped ->
            workout?.let {
                val sets = it.sets.map { item -> item.set }
                MiniPlayerState(
                    dayName = it.session.planDayName,
                    completedSets = sets.count { set -> set.completedAt != null },
                    totalSets = sets.size,
                    sets = sets,
                    skippedRestSetId = skipped,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
