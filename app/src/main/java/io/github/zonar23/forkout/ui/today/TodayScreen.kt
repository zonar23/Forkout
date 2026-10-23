package io.github.zonar23.forkout.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.relation.SessionOverview
import io.github.zonar23.forkout.ui.components.PlaceholderContent
import io.github.zonar23.forkout.ui.components.summaryLine
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

@Composable
fun TodayScreen(
    onOpenWorkout: () -> Unit,
    onEditDay: (Long) -> Unit,
    onOpenWorkoutDetail: (Long) -> Unit,
    onCreatePlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.openWorkout) {
        if (uiState.openWorkout) {
            viewModel.onWorkoutOpened()
            onOpenWorkout()
        }
    }

    if (uiState.isLoading) return

    val next = uiState.nextUp
    if (next == null && uiState.activeWorkoutName == null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxSize(),
        ) {
            PlaceholderContent(
                iconRes = R.drawable.ic_today,
                title = stringResource(R.string.nav_today),
                message = stringResource(R.string.today_empty),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onCreatePlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.today_create_plan))
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (next != null || uiState.activeWorkoutName != null) {
            TodayCard(title = stringResource(R.string.today_next)) {
                val running = uiState.activeWorkoutName
                if (running != null) {
                    Text(running, style = MaterialTheme.typography.headlineMedium)
                } else if (next != null) {
                    Text(next.plan.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(next.day.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = if (uiState.nextExerciseCount > 0) {
                            pluralStringResource(R.plurals.plan_day_exercise_count, uiState.nextExerciseCount, uiState.nextExerciseCount)
                        } else {
                            stringResource(R.string.today_no_exercises)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (running != null || uiState.nextExerciseCount > 0) {
                    Button(
                        onClick = viewModel::startOrResume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                        Text(
                            text = stringResource(if (running != null) R.string.today_resume else R.string.workout_start),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else if (next != null) {
                    OutlinedButton(
                        onClick = { onEditDay(next.day.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.today_edit_day))
                    }
                }
            }
        }

        TodayCard(title = stringResource(R.string.today_week)) {
            val target = uiState.weekTarget
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (target != null) {
                        stringResource(R.string.today_week_progress, uiState.weekDone, target)
                    } else {
                        uiState.weekDone.toString()
                    },
                    style = NumberTextStyles.large,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pluralStringResource(R.plurals.today_week_sessions, target ?: uiState.weekDone),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                )
            }
            if (target != null && target > 0) {
                LinearProgressIndicator(
                    progress = { (uiState.weekDone / target.toFloat()).coerceAtMost(1f) },
                    // The default track uses secondaryContainer, which is the accent in this theme.
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        uiState.lastWorkout?.let { last ->
            TodayCard(
                title = stringResource(R.string.today_last),
                modifier = Modifier.clickable { onOpenWorkoutDetail(last.session.id) },
            ) {
                Text(last.session.planDayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = last.summaryLine(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TodayCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

