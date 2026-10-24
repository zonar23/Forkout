package io.github.zonar23.forkout.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.WorkoutSetEntity
import io.github.zonar23.forkout.ui.components.ConfirmDeleteDialog
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.MenuAction
import io.github.zonar23.forkout.ui.components.NumberPadDialog
import io.github.zonar23.forkout.ui.components.OverflowMenu
import io.github.zonar23.forkout.ui.components.RepeatingStepButton
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
    val context = LocalContext.current
    val listState = rememberLazyListState()

    KeepScreenOn()
    LaunchedEffect(uiState.finishedSessionId) {
        uiState.finishedSessionId?.let(onFinished)
    }
    LaunchedEffect(uiState.isGone) {
        if (uiState.isGone) onClose()
    }
    // Auto-advance: bring the focus area back into view whenever the next set takes over.
    LaunchedEffect(uiState.focusedSetId) {
        if (uiState.focusedSetId != null) listState.animateScrollToItem(0)
    }

    val rest = uiState.allSets.currentRest(now, uiState.skippedRestSetId)
    val onToggle: (WorkoutSetEntity) -> Unit = { set ->
        if (set.completedAt == null) vibrateTick(context)
        viewModel.toggleComplete(set)
    }

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

        LinearProgressIndicator(
            progress = { if (uiState.allSets.isEmpty()) 0f else uiState.completedSets / uiState.allSets.size.toFloat() },
            // The default track uses secondaryContainer, which is the accent in this theme.
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "focus") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rest?.let {
                        RestRing(rest = it, now = now, onSkip = { viewModel.skipRest(it.afterSetId) })
                    }
                    val focus = uiState.focus
                    if (focus != null) {
                        FocusCard(
                            focus = focus,
                            isResting = rest != null,
                            onStepReps = { viewModel.stepReps(focus.set.id, it) },
                            onStepWeight = { viewModel.stepWeight(focus.set.id, it) },
                            onEditReps = { viewModel.openNumberInput(focus.set, NumberField.REPS) },
                            onEditWeight = { viewModel.openNumberInput(focus.set, NumberField.WEIGHT) },
                            onLikeLastTime = { viewModel.applyLastTime(focus.set.id) },
                            onComplete = { onToggle(focus.set) },
                        )
                    } else {
                        AllDoneCard()
                    }
                }
            }
            item(key = "overview") {
                Text(
                    text = stringResource(R.string.workout_overview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(uiState.exercises, key = { it.key }) { exercise ->
                OverviewCard(
                    exercise = exercise,
                    expanded = exercise.key in uiState.expandedKeys,
                    focusedSetId = uiState.focusedSetId,
                    isLast = exercise == uiState.exercises.last(),
                    onToggleExpanded = { viewModel.toggleExpanded(exercise.key) },
                    onFocus = viewModel::focus,
                    onToggle = onToggle,
                    onMoveToEnd = { viewModel.moveToEnd(exercise) },
                )
            }
        }

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

    uiState.numberInput?.let { input ->
        NumberPadDialog(
            title = stringResource(if (input.field == NumberField.REPS) R.string.set_edit_reps else R.string.set_edit_weight),
            initialValue = input.initialText,
            unit = stringResource(if (input.field == NumberField.REPS) R.string.workout_reps else R.string.workout_weight),
            allowDecimal = input.field == NumberField.WEIGHT,
            isValid = viewModel::isValidNumber,
            onConfirm = viewModel::confirmNumberInput,
            onDismiss = viewModel::dismissNumberInput,
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

/** The current set, large: exercise, set number, steppers and a thumb-sized check button. */
@Composable
private fun FocusCard(
    focus: Focus,
    isResting: Boolean,
    onStepReps: (Int) -> Unit,
    onStepWeight: (Int) -> Unit,
    onEditReps: () -> Unit,
    onEditWeight: () -> Unit,
    onLikeLastTime: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(if (isResting) R.string.workout_up_next else R.string.workout_now),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(focus.exercise.exercise.name, style = MaterialTheme.typography.headlineMedium)
            if (focus.exercise.exercise.setupNote.isNotBlank()) {
                Text(
                    text = focus.exercise.exercise.setupNote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.workout_set_of, focus.setNumber, focus.exercise.sets.size),
                style = NumberTextStyles.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            focus.lastTime?.let { last ->
                AssistChip(
                    onClick = onLikeLastTime,
                    label = {
                        Text(
                            stringResource(
                                R.string.workout_like_last_time,
                                stringResource(
                                    R.string.workout_last_set,
                                    last.actualReps ?: 0,
                                    WeightFormat.formatKg(last.actualWeightGrams ?: 0),
                                ),
                            )
                        )
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_history), contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            ValueStepper(
                value = (focus.set.actualReps ?: 0).toString(),
                unit = stringResource(R.string.workout_reps),
                onDecrease = { onStepReps(-REPS_STEP) },
                onIncrease = { onStepReps(REPS_STEP) },
                onValueClick = onEditReps,
            )
            ValueStepper(
                value = WeightFormat.formatKg(focus.set.actualWeightGrams ?: 0),
                unit = stringResource(R.string.workout_weight),
                onDecrease = { onStepWeight(-WEIGHT_STEP_GRAMS) },
                onIncrease = { onStepWeight(WEIGHT_STEP_GRAMS) },
                onValueClick = onEditWeight,
            )
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = 72.dp),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(28.dp))
                Text(
                    text = stringResource(R.string.workout_complete_set),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AllDoneCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(stringResource(R.string.workout_all_done), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.workout_all_done_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Large countdown ring after a completed set. Buzzes 10 s before the end and at the end,
 * as long as it is shown.
 */
@Composable
private fun RestRing(rest: RestTimer, now: Instant, onSkip: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(rest.afterSetId, rest.endsAt) {
        val untilEnd = Duration.between(Instant.now(), rest.endsAt).toMillis()
        val untilWarning = untilEnd - REST_WARNING_SECONDS * 1000L
        // Only buzz for moments that actually happen while the user is watching.
        if (untilWarning > 0 && rest.totalSeconds > REST_WARNING_SECONDS * 2) {
            delay(untilWarning)
            vibrateRestWarning(context)
        }
        val remaining = Duration.between(Instant.now(), rest.endsAt).toMillis()
        if (remaining > 0) {
            delay(remaining)
            vibrateRestOver(context)
        }
    }
    val remainingMillis = Duration.between(now, rest.endsAt).toMillis().coerceAtLeast(0)
    val fraction = remainingMillis / (rest.totalSeconds * 1000f)
    val warning = remainingMillis <= REST_WARNING_SECONDS * 1000L
    val ringColor = if (warning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
                    drawArc(ringColor, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.workout_rest),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = TimeFormat.formatMinutesSeconds(((remainingMillis + 999) / 1000).toInt()),
                        style = NumberTextStyles.large,
                        color = ringColor,
                    )
                }
            }
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = TouchTarget)) {
                Text(stringResource(R.string.workout_rest_skip), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** One exercise in the overview: collapsed to a progress line, or expanded with all sets. */
@Composable
private fun OverviewCard(
    exercise: WorkoutExercise,
    expanded: Boolean,
    focusedSetId: Long?,
    isLast: Boolean,
    onToggleExpanded: () -> Unit,
    onFocus: (Long) -> Unit,
    onToggle: (WorkoutSetEntity) -> Unit,
    onMoveToEnd: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchTarget)
                .clickable(onClick = onToggleExpanded)
                .padding(start = 16.dp),
        ) {
            if (exercise.isDone) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = stringResource(R.string.workout_set_done),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.workout_sets_progress, exercise.completedCount, exercise.sets.size),
                    style = NumberTextStyles.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
            )
            if (!exercise.isDone && !isLast) {
                OverflowMenu(
                    actions = listOf(
                        MenuAction(stringResource(R.string.workout_move_to_end), R.drawable.ic_arrow_downward, onClick = onMoveToEnd),
                    ),
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
        }
        if (expanded) {
            exercise.sets.forEachIndexed { index, set ->
                SetRow(
                    number = index + 1,
                    set = set,
                    isFocused = set.id == focusedSetId,
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
    isFocused: Boolean,
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
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.workout_last_set,
                set.actualReps ?: 0,
                WeightFormat.formatKg(set.actualWeightGrams ?: 0),
            ),
            style = NumberTextStyles.medium,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ValueStepper(
    value: String,
    unit: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onValueClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingStepButton(R.drawable.ic_remove, stringResource(R.string.stepper_decrease), onDecrease, size = 64.dp)
        Surface(
            onClick = onValueClick,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .heightIn(min = 72.dp)
                    .padding(top = 8.dp),
            ) {
                Text(value, style = NumberTextStyles.large)
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                )
            }
        }
        RepeatingStepButton(R.drawable.ic_add, stringResource(R.string.stepper_increase), onIncrease, size = 64.dp)
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
