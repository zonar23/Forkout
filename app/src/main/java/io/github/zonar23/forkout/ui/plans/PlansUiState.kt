package io.github.zonar23.forkout.ui.plans

import io.github.zonar23.forkout.data.db.entity.PlanEntity

data class PlansUiState(
    val plans: List<PlanEntity> = emptyList(),
    val isLoading: Boolean = true,
)
