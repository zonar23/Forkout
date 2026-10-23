package io.github.zonar23.forkout.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.ConfirmDeleteDialog
import io.github.zonar23.forkout.ui.components.DateFormat
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.MenuAction
import io.github.zonar23.forkout.ui.components.OverflowMenu
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.theme.NumberTextStyles
import io.github.zonar23.forkout.ui.workout.WorkoutStatsRow

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = uiState.session

    LaunchedEffect(uiState.isGone) {
        if (uiState.isGone) onBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = session?.planDayName.orEmpty(),
            onBack = onBack,
            actions = {
                OverflowMenu(
                    actions = listOf(
                        MenuAction(
                            label = stringResource(R.string.history_delete),
                            iconRes = R.drawable.ic_delete,
                            isDestructive = true,
                            onClick = viewModel::onDeleteClick,
                        ),
                    ),
                )
            },
        )
        if (session == null) return@Column

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = DateFormat.formatDate(session.startedAt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.summary?.let { WorkoutStatsRow(it) }
                }
            }
            items(uiState.exercises, key = { it.position }) { exercise ->
                val completed = exercise.sets.filter { it.completedAt != null }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.workout_sets_progress, completed.size, exercise.sets.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        completed.forEachIndexed { index, set ->
                            Row {
                                Text(
                                    text = (index + 1).toString(),
                                    style = NumberTextStyles.small,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.workout_last_set,
                                        set.actualReps ?: 0,
                                        WeightFormat.formatKg(set.actualWeightGrams ?: 0),
                                    ),
                                    style = NumberTextStyles.small,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.history_delete_dialog_title),
            text = stringResource(R.string.history_delete_dialog_text),
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }
}
