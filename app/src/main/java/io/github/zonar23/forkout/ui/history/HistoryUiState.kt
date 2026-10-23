package io.github.zonar23.forkout.ui.history

import io.github.zonar23.forkout.data.db.relation.SessionOverview

data class HistoryUiState(
    val isLoading: Boolean = true,
    val workouts: List<SessionOverview> = emptyList(),
)
