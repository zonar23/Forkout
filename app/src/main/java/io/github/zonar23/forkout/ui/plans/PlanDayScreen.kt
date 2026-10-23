package io.github.zonar23.forkout.ui.plans

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.PlanExerciseEntity
import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import io.github.zonar23.forkout.data.db.relation.PlanExerciseWithDetails
import io.github.zonar23.forkout.ui.components.ConfirmDeleteDialog
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.MenuAction
import io.github.zonar23.forkout.ui.components.NumberStepperField
import io.github.zonar23.forkout.ui.components.OverflowMenu
import io.github.zonar23.forkout.ui.components.TimeFormat
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.plans.SetEditorState.Companion.REPS_STEP
import io.github.zonar23.forkout.ui.plans.SetEditorState.Companion.REST_STEP_SECONDS
import io.github.zonar23.forkout.ui.plans.SetEditorState.Companion.WEIGHT_STEP_GRAMS
import io.github.zonar23.forkout.ui.theme.NumberTextStyles
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlanDayScreen(
    onBack: () -> Unit,
    onAddExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanDayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val day = uiState.day

    LaunchedEffect(uiState.isGone) {
        if (uiState.isGone) onBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(title = day?.name.orEmpty(), onBack = onBack)
        if (day == null) return@Column

        Box(modifier = Modifier.fillMaxSize()) {
            ExerciseList(
                exercises = uiState.exercises,
                onReorder = viewModel::onReorder,
                onSetClick = viewModel::openSet,
                onAddSet = viewModel::addSet,
                onRemove = viewModel::startRemove,
            )
            FloatingActionButton(
                onClick = { onAddExercise(day.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.day_add_exercise),
                )
            }
        }
    }

    uiState.setEditor?.let { editor ->
        SetEditorDialog(
            editor = editor,
            onRepsChange = viewModel::onRepsChange,
            onWeightChange = viewModel::onWeightChange,
            onRestChange = viewModel::onRestChange,
            onStepReps = viewModel::stepReps,
            onStepWeight = viewModel::stepWeight,
            onStepRest = viewModel::stepRest,
            onApplyToAllChange = viewModel::onApplyToAllChange,
            onSave = viewModel::saveSet,
            onDelete = viewModel::deleteSet,
            onDismiss = viewModel::dismissSet,
        )
    }
    uiState.removing?.let {
        ConfirmDeleteDialog(
            title = stringResource(R.string.day_remove_exercise),
            text = uiState.exercises.firstOrNull { item -> item.planExercise.id == it.id }?.exercise?.name.orEmpty(),
            onConfirm = viewModel::confirmRemove,
            onDismiss = viewModel::dismissRemove,
        )
    }
}

/** Exercises sortable by their drag handle; the order is saved when the drag stops. */
@Composable
private fun ExerciseList(
    exercises: List<PlanExerciseWithDetails>,
    onReorder: (List<PlanExerciseEntity>) -> Unit,
    onSetClick: (set: PlanSetEntity, setNumber: Int, setCount: Int) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemove: (PlanExerciseEntity) -> Unit,
) {
    var items by remember(exercises) { mutableStateOf(exercises) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = items.indexOfFirst { it.planExercise.id == from.key }
        val toIndex = items.indexOfFirst { it.planExercise.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            items = items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.day_exercises_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.planExercise.id }) { item ->
            ReorderableItem(reorderState, key = item.planExercise.id) { isDragging ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDragging) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(
                                onDragStopped = { onReorder(items.map { it.planExercise }) },
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_indicator),
                                contentDescription = stringResource(R.string.drag_to_reorder),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                            if (item.exercise.setupNote.isNotBlank()) {
                                Text(
                                    text = item.exercise.setupNote,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        OverflowMenu(
                            actions = listOf(
                                MenuAction(
                                    label = stringResource(R.string.day_remove_exercise),
                                    iconRes = R.drawable.ic_delete,
                                    isDestructive = true,
                                ) { onRemove(item.planExercise) },
                            ),
                        )
                    }
                    item.sets.forEachIndexed { index, set ->
                        SetRow(
                            number = index + 1,
                            set = set,
                            onClick = { onSetClick(set, index + 1, item.sets.size) },
                        )
                    }
                    TextButton(
                        onClick = { onAddSet(item.planExercise.id) },
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Text(stringResource(R.string.day_add_set), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRow(number: Int, set: PlanSetEntity, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = number.toString(),
            style = NumberTextStyles.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.day_set_reps, set.targetReps, set.targetReps),
            style = NumberTextStyles.small,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.day_set_weight, WeightFormat.formatKg(set.targetWeightGrams)),
            style = NumberTextStyles.small,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = TimeFormat.formatMinutesSeconds(set.restSeconds),
            style = NumberTextStyles.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SetEditorDialog(
    editor: SetEditorState,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onRestChange: (String) -> Unit,
    onStepReps: (Int) -> Unit,
    onStepWeight: (Int) -> Unit,
    onStepRest: (Int) -> Unit,
    onApplyToAllChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_edit_title, editor.setNumber)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                NumberStepperField(
                    value = editor.repsText,
                    onValueChange = onRepsChange,
                    onDecrease = { onStepReps(-REPS_STEP) },
                    onIncrease = { onStepReps(REPS_STEP) },
                    label = stringResource(R.string.set_edit_reps),
                    isError = editor.reps == null,
                )
                NumberStepperField(
                    value = editor.weightText,
                    onValueChange = onWeightChange,
                    onDecrease = { onStepWeight(-WEIGHT_STEP_GRAMS) },
                    onIncrease = { onStepWeight(WEIGHT_STEP_GRAMS) },
                    label = stringResource(R.string.set_edit_weight),
                    isError = editor.weightGrams == null,
                    keyboardType = KeyboardType.Decimal,
                )
                NumberStepperField(
                    value = editor.restText,
                    onValueChange = onRestChange,
                    onDecrease = { onStepRest(-REST_STEP_SECONDS) },
                    onIncrease = { onStepRest(REST_STEP_SECONDS) },
                    label = stringResource(R.string.set_edit_rest),
                    isError = editor.restSeconds == null,
                )
                if (editor.setCount > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplyToAllChange(!editor.applyToAll) },
                    ) {
                        Checkbox(checked = editor.applyToAll, onCheckedChange = onApplyToAllChange)
                        Text(stringResource(R.string.set_edit_apply_all))
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.set_edit_delete),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = editor.isValid) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
