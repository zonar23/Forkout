package io.github.zonar23.forkout.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.data.repository.ExerciseHistoryEntry
import io.github.zonar23.forkout.ui.components.DateFormat
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.WeightFormat
import io.github.zonar23.forkout.ui.components.labelRes
import io.github.zonar23.forkout.ui.theme.ForkoutTheme
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

@Composable
fun ExerciseEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    ExerciseEditContent(
        uiState = uiState,
        history = history,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeSelect = viewModel::onTypeSelect,
        onMuscleGroupToggle = viewModel::onMuscleGroupToggle,
        onSetupNoteChange = viewModel::onSetupNoteChange,
        onSave = viewModel::save,
        onRemoveClick = viewModel::onRemoveClick,
        onRestoreClick = viewModel::restore,
        modifier = modifier,
    )

    if (uiState.showRemoveDialog) {
        RemoveDialog(onConfirm = viewModel::confirmRemove, onDismiss = viewModel::onRemoveDismiss)
    }
}

@Composable
private fun ExerciseEditContent(
    uiState: ExerciseEditUiState,
    history: List<ExerciseHistoryEntry>,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeSelect: (ExerciseType) -> Unit,
    onMuscleGroupToggle: (MuscleGroup) -> Unit,
    onSetupNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemoveClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(
                if (uiState.isNew) R.string.exercise_edit_title_new else R.string.exercise_edit_title
            ),
            onBack = onBack,
            actions = {
                IconButton(onClick = onSave, enabled = !uiState.isLoading) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.exercise_edit_save),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        if (uiState.isLoading) return@Column

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                // Keeps focused fields above the keyboard (edge-to-edge does not resize).
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.exercise_edit_name)) },
                singleLine = true,
                isError = uiState.showNameError,
                supportingText = if (uiState.showNameError) {
                    { Text(stringResource(R.string.exercise_edit_name_required)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Section(title = stringResource(R.string.exercise_edit_type)) {
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { onTypeSelect(type) },
                        label = { Text(stringResource(type.labelRes)) },
                    )
                }
            }

            Section(title = stringResource(R.string.exercise_edit_muscle_groups)) {
                MuscleGroup.entries.forEach { group ->
                    FilterChip(
                        selected = group in uiState.muscleGroups,
                        onClick = { onMuscleGroupToggle(group) },
                        label = { Text(stringResource(group.labelRes)) },
                    )
                }
            }

            OutlinedTextField(
                value = uiState.setupNote,
                onValueChange = onSetupNoteChange,
                label = { Text(stringResource(R.string.exercise_edit_setup_note)) },
                placeholder = { Text(stringResource(R.string.exercise_edit_setup_note_hint)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!uiState.isNew) {
                ExerciseHistory(history)
            }

            if (!uiState.isNew) {
                if (uiState.isArchived) {
                    OutlinedButton(
                        onClick = onRestoreClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_unarchive), contentDescription = null)
                        Text(
                            text = stringResource(R.string.exercise_edit_restore),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRemoveClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                        Text(
                            text = stringResource(R.string.exercise_edit_remove),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

/** Recent workouts with this exercise: date and completed sets, newest first. */
@Composable
private fun ExerciseHistory(history: List<ExerciseHistoryEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.exercise_history_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.exercise_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val resources = LocalResources.current
        history.forEach { entry ->
            Column {
                Text(DateFormat.formatDate(entry.startedAt), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = entry.sets.joinToString(", ") {
                        resources.getString(
                            R.string.workout_last_set,
                            it.actualReps ?: 0,
                            WeightFormat.formatKg(it.actualWeightGrams ?: 0),
                        )
                    },
                    style = NumberTextStyles.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RemoveDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exercise_remove_dialog_title)) },
        text = { Text(stringResource(R.string.exercise_remove_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.exercise_remove_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun ExerciseEditContentPreview() {
    ForkoutTheme {
        ExerciseEditContent(
            uiState = ExerciseEditUiState(
                isNew = false,
                name = "Beinpresse",
                muscleGroups = setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES),
                setupNote = "Sitz Stufe 4",
            ),
            history = emptyList(),
            onBack = {},
            onNameChange = {},
            onTypeSelect = {},
            onMuscleGroupToggle = {},
            onSetupNoteChange = {},
            onSave = {},
            onRemoveClick = {},
            onRestoreClick = {},
        )
    }
}
