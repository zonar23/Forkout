package io.github.zonar23.forkout.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.PlaceholderContent
import io.github.zonar23.forkout.ui.components.summaryLine

@Composable
fun HistoryScreen(
    onWorkoutClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isLoading && uiState.workouts.isEmpty()) {
        PlaceholderContent(
            iconRes = R.drawable.ic_history,
            title = stringResource(R.string.nav_history),
            message = stringResource(R.string.history_empty),
            modifier = modifier,
        )
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(uiState.workouts, key = { it.session.id }) { workout ->
            ListItem(
                headlineContent = { Text(workout.session.planDayName) },
                supportingContent = { Text(workout.summaryLine()) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable { onWorkoutClick(workout.session.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    }
}
