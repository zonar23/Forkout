package io.github.zonar23.forkout.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.ui.components.ConfirmDeleteDialog
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.MenuAction
import io.github.zonar23.forkout.ui.components.OverflowMenu
import io.github.zonar23.forkout.ui.components.TextInputDialog
import io.github.zonar23.forkout.ui.components.TimeFormat
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.theme.NumberTextStyles
import io.github.zonar23.forkout.ui.workout.WorkoutViewModel.Companion.REPS_STEP
import io.github.zonar23.forkout.ui.workout.WorkoutViewModel.Companion.WEIGHT_STEP_GRAMS
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Touch targets in workout mode are at least 56 dp. */
private val TouchTarget = 56.dp

@Composable
fun WorkoutScreen(
    onClose: () -> Unit,
    onFinished: (sessionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now by rememberNow()
    val session = uiState.session

    KeepScreenOn()
    LaunchedEffect(uiState.finishedSessionId) {
        uiState.finishedSessionId?.let(onFinished)
    }
    LaunchedEffect(uiState.isGone) {
        if (uiState.isGone) onClose()
    }

    val rest = uiState.allSets.currentRest(now, uiState.skippedRestSetId)

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = session?.planDayName.orEmpty(),
            onBack = onClose,
            actions = {
                if (session != null) {
                    Text(
                        text = TimeFormat.formatElapsed(Duration.between(session.startedAt, now).seconds),
                        style = NumberTextStyles.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OverflowMenu(
                        actions = listOf(
                            MenuAction(
                                label = stringResource(R.string.workout_discard),
                                iconRes = R.drawable.ic_delete,
                                isDestructive = true,
                                onClick = viewModel::requestDiscard,
                            ),
                        ),
                    )
                }
            },
        )
        if (session == null) return@Column

        Text(
            text = stringResource(R.string.workout_sets_progress, uiState.completedSets, uiState.allSets.size),
            style = NumberTextStyles.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LinearProgressIndicator(
            progress = { if (uiState.allSets.isEmpty()) 0f else uiState.completedSets / uiState.allSets.size.toFloat() },
            // The default track uses secondaryContainer, which is the accent in this theme.
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(uiState.exercises, key = { it.position }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    focusedSetId = uiState.focusedSetId,
                    onFocus = viewModel::focus,
                    onToggle = viewModel::toggleComplete,
                    onStepReps = viewModel::stepReps,
                    onStepWeight = viewModel::stepWeight,
                    onEditWeight = viewModel::openWeightInput,
                )
            }
        }

        rest?.let { RestBar(rest = it, now = now, onSkip = { viewModel.skipRest(it.afterSetId) }) }

        Button(
            onClick = viewModel::requestFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = TouchTarget),
        ) {
            Text(stringResource(R.string.workout_finish))
        }
    }

    uiState.weightInput?.let { input ->
        TextInputDialog(
            title = stringResource(R.string.set_edit_weight),
            value = input.text,
            onValueChange = viewModel::onWeightInputChange,
            onConfirm = viewModel::confirmWeightInput,
            onDismiss = viewModel::dismissWeightInput,
            keyboardType = KeyboardType.Decimal,
            isValid = WeightFormat.parseKg(input.text) != null,
        )
    }
    if (uiState.showFinishDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinish,
            title = { Text(stringResource(R.string.workout_finish_dialog_title)) },
            text = {
                Text(pluralStringResource(R.plurals.workout_finish_dialog_text, uiState.openSets, uiState.openSets))
            },
            confirmButton = {
                TextButton(onClick = viewModel::finish) { Text(stringResource(R.string.workout_finish_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFinish) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
    if (uiState.showDiscardDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.workout_discard_dialog_title),
            text = stringResource(R.string.workout_discard_dialog_text),
            onConfirm = viewModel::confirmDiscard,
            onDismiss = viewModel::dismissDiscard,
            confirmLabel = stringResource(R.string.workout_discard_confirm),
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    focusedSetId: Long?,
    onFocus: (Long) -> Unit,
    onToggle: (WorkoutSetEntity) -> Unit,
    onStepReps: (WorkoutSetEntity, Int) -> Unit,
    onStepWeight: (WorkoutSetEntity, Int) -> Unit,
    onEditWeight: (WorkoutSetEntity) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Text(exercise.exercise.name, style = MaterialTheme.typography.titleLarge)
            if (exercise.exercise.setupNote.isNotBlank()) {
                Text(
                    text = exercise.exercise.setupNote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (exercise.lastSets.isNotEmpty()) {
                val resources = LocalResources.current
                val last = exercise.lastSets.joinToString(", ") {
                    resources.getString(
                        R.string.workout_last_set,
                        it.actualReps ?: 0,
                        WeightFormat.formatKg(it.actualWeightGrams ?: 0),
                    )
                }
                Text(
                    text = stringResource(R.string.workout_last_time, last),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        exercise.sets.forEachIndexed { index, set ->
            if (set.id == focusedSetId) {
                FocusedSet(
                    number = index + 1,
                    set = set,
                    onToggle = { onToggle(set) },
                    onStepReps = { onStepReps(set, it) },
                    onStepWeight = { onStepWeight(set, it) },
                    onEditWeight = { onEditWeight(set) },
                )
            } else {
                SetRow(
                    number = index + 1,
                    set = set,
                    onClick = { if (set.completedAt == null) onFocus(set.id) },
                    onToggle = { onToggle(set) },
                )
            }
        }
    }
}

/** Compact row: check button, set number and values. Completed sets are dimmed. */
@Composable
private fun SetRow(
    number: Int,
    set: WorkoutSetEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val done = set.completedAt != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
            .alpha(if (done) 0.6f else 1f),
    ) {
        CheckButton(done = done, onClick = onToggle)
        Text(
            text = number.toString(),
            style = NumberTextStyles.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.workout_last_set,
                set.actualReps ?: 0,
                WeightFormat.formatKg(set.actualWeightGrams ?: 0),
            ),
            style = NumberTextStyles.medium,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Expanded set with steppers for reps and weight and a large complete button. */
@Composable
private fun FocusedSet(
    number: Int,
    set: WorkoutSetEntity,
    onToggle: () -> Unit,
    onStepReps: (Int) -> Unit,
    onStepWeight: (Int) -> Unit,
    onEditWeight: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Explicit, because contentColorFor() maps this surface to the accent color.
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.set_edit_title, number),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ValueStepper(
                value = (set.actualReps ?: 0).toString(),
                unit = stringResource(R.string.workout_reps),
                onDecrease = { onStepReps(-REPS_STEP) },
                onIncrease = { onStepReps(REPS_STEP) },
            )
            ValueStepper(
                value = WeightFormat.formatKg(set.actualWeightGrams ?: 0),
                unit = stringResource(R.string.workout_weight),
                onDecrease = { onStepWeight(-WEIGHT_STEP_GRAMS) },
                onIncrease = { onStepWeight(WEIGHT_STEP_GRAMS) },
                onValueClick = onEditWeight,
            )
            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                Text(stringResource(R.string.workout_complete_set), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun ValueStepper(
    value: String,
    unit: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onValueClick: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(R.drawable.ic_remove, stringResource(R.string.stepper_decrease), onDecrease)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TouchTarget)
                .then(if (onValueClick != null) Modifier.clickable(onClick = onValueClick) else Modifier)
                .padding(top = 8.dp),
        ) {
            Text(value, style = NumberTextStyles.large)
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
        StepButton(R.drawable.ic_add, stringResource(R.string.stepper_increase), onIncrease)
    }
}

@Composable
private fun StepButton(iconRes: Int, description: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(TouchTarget),
    ) {
        Icon(painterResource(iconRes), contentDescription = description)
    }
}

@Composable
private fun CheckButton(done: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(TouchTarget)) {
        Icon(
            painter = painterResource(if (done) R.drawable.ic_check_circle else R.drawable.ic_radio_button_unchecked),
            contentDescription = stringResource(if (done) R.string.workout_set_done else R.string.workout_set_open),
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** Countdown above the finish button. Vibrates once when the rest ends while it is shown. */
@Composable
private fun RestBar(rest: RestTimer, now: Instant, onSkip: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(rest.afterSetId, rest.endsAt) {
        val wait = Duration.between(Instant.now(), rest.endsAt).toMillis()
        // Only buzz if the rest actually ends while the user is watching.
        if (wait > 0) {
            delay(wait)
            vibrateRestOver(context)
        }
    }
    val remaining = Duration.between(now, rest.endsAt).seconds.coerceAtLeast(0)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Explicit, because contentColorFor() maps this surface to the accent color.
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.workout_rest),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = TimeFormat.formatMinutesSeconds(remaining.toInt()),
                    style = NumberTextStyles.large,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                )
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = TouchTarget)) {
                    Text(stringResource(R.string.workout_rest_skip))
                }
            }
            LinearProgressIndicator(
                progress = { remaining / rest.totalSeconds.toFloat() },
                trackColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
