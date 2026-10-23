package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.PlanDayEntity
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.ui.navigation.PlanDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlanRepository,
) : ViewModel() {

    private val planId = savedStateHandle.toRoute<PlanDetailRoute>().planId

    /** Dialog and event state that does not come from the database. */
    private val local = MutableStateFlow(PlanDetailUiState(isLoading = false))

    val uiState: StateFlow<PlanDetailUiState> = combine(
        repository.observePlan(planId),
        repository.observeExerciseCounts(planId),
        local,
    ) { plan, counts, local ->
        local.copy(
            isLoading = false,
            plan = plan?.plan,
            days = plan?.days.orEmpty().map { PlanDayItem(it, counts[it.id] ?: 0) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanDetailUiState())

    fun addDay(name: String) {
        viewModelScope.launch { repository.addDay(planId, name) }
    }

    /** Persists the order after a drag gesture. */
    fun onReorder(days: List<PlanDayEntity>) {
        viewModelScope.launch { repository.reorderDays(days) }
    }

    fun copyDay(day: PlanDayEntity, newName: String) {
        viewModelScope.launch { repository.copyDay(day.id, newName) }
    }

    fun copyPlan(newName: String) {
        viewModelScope.launch {
            val copyId = repository.copyPlan(planId, newName)
            local.update { it.copy(copiedPlanId = copyId) }
        }
    }

    fun onCopiedPlanOpened() = local.update { it.copy(copiedPlanId = null) }

    fun startRename(day: PlanDayEntity) = local.update { it.copy(renaming = day, renameText = day.name) }

    fun onRenameTextChange(text: String) = local.update { it.copy(renameText = text) }

    fun dismissRename() = local.update { it.copy(renaming = null, renameText = "") }

    fun confirmRename() {
        val state = local.value
        val day = state.renaming ?: return
        val name = state.renameText.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            repository.renameDay(day, name)
            dismissRename()
        }
    }

    fun startDelete(day: PlanDayEntity) = local.update { it.copy(deleting = day) }

    fun dismissDelete() = local.update { it.copy(deleting = null) }

    fun confirmDelete() {
        val day = local.value.deleting ?: return
        viewModelScope.launch {
            repository.deleteDay(day)
            dismissDelete()
        }
    }
}
