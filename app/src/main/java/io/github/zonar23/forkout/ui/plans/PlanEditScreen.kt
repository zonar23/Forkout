package io.github.zonar23.forkout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.DetailTopBar
import io.github.zonar23.forkout.ui.components.NumberStepperField
import io.github.zonar23.forkout.ui.theme.ForkoutTheme

@Composable
fun PlanEditScreen(
    onBack: () -> Unit,
    onResult: (PlanEditResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.result) {
        uiState.result?.let(onResult)
    }

    PlanEditContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onGoalChange = viewModel::onGoalChange,
        onSessionsChange = viewModel::onSessionsChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::save,
        onDeleteClick = viewModel::onDeleteClick,
        modifier = modifier,
    )

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text(stringResource(R.string.plan_delete_dialog_title)) },
            text = { Text(stringResource(R.string.plan_delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlanEditContent(
    uiState: PlanEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onSessionsChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(if (uiState.isNew) R.string.plan_edit_title_new else R.string.plan_edit_title),
            onBack = onBack,
            actions = {
                IconButton(onClick = onSave, enabled = !uiState.isLoading) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.dialog_save),
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.plan_edit_name)) },
                placeholder = { Text(stringResource(R.string.plan_edit_name_hint)) },
                singleLine = true,
                isError = uiState.showNameError,
                supportingText = if (uiState.showNameError) {
                    { Text(stringResource(R.string.plan_edit_name_required)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.goal,
                onValueChange = onGoalChange,
                label = { Text(stringResource(R.string.plan_edit_goal)) },
                placeholder = { Text(stringResource(R.string.plan_edit_goal_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            NumberStepperField(
                value = uiState.sessionsPerWeek.toString(),
                // Only the buttons change the value, which keeps it within 1–7.
                onValueChange = {},
                onDecrease = { onSessionsChange(-1) },
                onIncrease = { onSessionsChange(1) },
                label = stringResource(R.string.plan_edit_sessions),
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.plan_edit_note)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!uiState.isNew) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                    Text(stringResource(R.string.plan_edit_delete), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun PlanEditContentPreview() {
    ForkoutTheme {
        PlanEditContent(
            uiState = PlanEditUiState(isNew = false, name = "Coach-Plan Herbst 2026", goal = "Muskelaufbau"),
            onBack = {},
            onNameChange = {},
            onGoalChange = {},
            onSessionsChange = {},
            onNoteChange = {},
            onSave = {},
            onDeleteClick = {},
        )
    }
}
