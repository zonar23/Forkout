package io.github.zonar23.forkout.ui.plans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.data.repository.PlanRepository
import io.github.zonar23.forkout.ui.navigation.PlanEditRoute
import io.github.zonar23.forkout.ui.plans.PlanEditUiState.Companion.MAX_SESSIONS_PER_WEEK
import io.github.zonar23.forkout.ui.plans.PlanEditUiState.Companion.MIN_SESSIONS_PER_WEEK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PlanRepository,
) : ViewModel() {

    private val planId = savedStateHandle.toRoute<PlanEditRoute>().planId
    private val isNew = planId == PlanEditRoute.NEW_ID
    private var original: PlanEntity? = null

    private val _uiState = MutableStateFlow(PlanEditUiState(isNew = isNew, isLoading = !isNew))
    val uiState: StateFlow<PlanEditUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                val plan = repository.getPlan(planId)
                original = plan
                _uiState.update { state ->
                    if (plan == null) {
                        state.copy(isLoading = false, result = PlanEditResult.NotFound)
                    } else {
                        state.copy(
                            isLoading = false,
                            name = plan.name,
                            goal = plan.goal,
                            sessionsPerWeek = plan.sessionsPerWeek,
                            note = plan.note,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, showNameError = false) }

    fun onGoalChange(goal: String) = _uiState.update { it.copy(goal = goal) }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onSessionsChange(delta: Int) = _uiState.update {
        it.copy(sessionsPerWeek = (it.sessionsPerWeek + delta).coerceIn(MIN_SESSIONS_PER_WEEK, MAX_SESSIONS_PER_WEEK))
    }

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            val base = original
            val result = if (base == null) {
                PlanEditResult.Created(
                    repository.createPlan(
                        name = name,
                        sessionsPerWeek = state.sessionsPerWeek,
                        goal = state.goal.trim(),
                        note = state.note.trim(),
                    )
                )
            } else {
                repository.updatePlan(
                    base.copy(
                        name = name,
                        goal = state.goal.trim(),
                        sessionsPerWeek = state.sessionsPerWeek,
                        note = state.note.trim(),
                    )
                )
                PlanEditResult.Saved
            }
            _uiState.update { it.copy(result = result) }
        }
    }

    fun onDeleteClick() = _uiState.update { it.copy(showDeleteDialog = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun confirmDelete() {
        val plan = original ?: return
        viewModelScope.launch {
            repository.deletePlan(plan)
            _uiState.update { it.copy(showDeleteDialog = false, result = PlanEditResult.Deleted) }
        }
    }
}
