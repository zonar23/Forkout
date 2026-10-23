package io.github.zonar23.forkout.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.StatTile
import io.github.zonar23.forkout.ui.components.TimeFormat
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .size(72.dp),
            )
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineMedium)
            Text(uiState.dayName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (summary != null) {
                WorkoutStatsRow(summary)
                summary.exercises.forEach { exercise ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.workout_sets_progress, exercise.completedSets, exercise.totalSets),
                            style = NumberTextStyles.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.summary_done))
        }
    }
}

/** Duration, completed sets and volume side by side. Also used in the history detail. */
@Composable
fun WorkoutStatsRow(summary: WorkoutSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(
            label = stringResource(R.string.summary_duration),
            value = TimeFormat.formatElapsed(summary.duration.seconds),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.summary_sets),
            value = "${summary.completedSets}/${summary.totalSets}",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.summary_volume),
            value = formatVolumeKg(summary.volumeGrams),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Volume is stored as gram × reps; formatKg turns it into kg × reps. */
fun formatVolumeKg(volumeGrams: Long): String =
    WeightFormat.formatKg(volumeGrams.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
