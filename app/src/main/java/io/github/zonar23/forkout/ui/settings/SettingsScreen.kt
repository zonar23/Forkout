package io.github.zonar23.forkout.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.TimeFormat
import io.github.zonar23.forkout.ui.settings.SettingsViewModel.Companion.REST_STEP_SECONDS
import io.github.zonar23.forkout.ui.theme.ForkoutTheme
import io.github.zonar23.forkout.ui.theme.NumberTextStyles

/** Most file managers label JSON correctly; some only offer it as plain text or binary. */
private val ImportMimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::readImport)
    }
    val fileName = stringResource(R.string.backup_file_name, viewModel.backupDate())

    SettingsContent(
        uiState = uiState,
        onAmoledBlackChange = viewModel::setAmoledBlack,
        onStepRest = viewModel::stepDefaultRest,
        onExport = { exportLauncher.launch(fileName) },
        onImport = { importLauncher.launch(ImportMimeTypes) },
        modifier = modifier,
    )

    uiState.pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImport,
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.import_confirm_text,
                        pending.summary.exercises,
                        pending.summary.plans,
                        pending.summary.workouts,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.import_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissImport) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(stringResource(message.textRes)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.dialog_ok)) }
            },
        )
    }
}

private val SettingsMessage.textRes: Int
    get() = when (this) {
        SettingsMessage.EXPORT_DONE -> R.string.export_done
        SettingsMessage.IMPORT_DONE -> R.string.import_done
        SettingsMessage.INVALID_FILE -> R.string.backup_error_invalid
        SettingsMessage.UNSUPPORTED_VERSION -> R.string.backup_error_version
        SettingsMessage.IO_ERROR -> R.string.backup_error_io
    }

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAmoledBlackChange: (Boolean) -> Unit,
    onStepRest: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (uiState.isBusy) {
            LinearProgressIndicator(
                // The default track uses secondaryContainer, which is the accent in this theme.
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionTitle(stringResource(R.string.settings_section_display))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_amoled_title)) },
            supportingContent = { Text(stringResource(R.string.settings_amoled_summary)) },
            trailingContent = {
                // The whole row toggles; the switch only shows the state.
                Switch(checked = uiState.amoledBlack, onCheckedChange = null)
            },
            colors = itemColors,
            modifier = Modifier.toggleable(
                value = uiState.amoledBlack,
                role = Role.Switch,
                onValueChange = onAmoledBlackChange,
            ),
        )

        SectionTitle(stringResource(R.string.settings_section_training))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_default_rest_title)) },
            supportingContent = { Text(stringResource(R.string.settings_default_rest_summary)) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton(R.drawable.ic_remove, stringResource(R.string.stepper_decrease)) {
                        onStepRest(-REST_STEP_SECONDS)
                    }
                    Text(
                        text = TimeFormat.formatMinutesSeconds(uiState.defaultRestSeconds),
                        style = NumberTextStyles.small,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    StepButton(R.drawable.ic_add, stringResource(R.string.stepper_increase)) {
                        onStepRest(REST_STEP_SECONDS)
                    }
                }
            },
            colors = itemColors,
        )

        SectionTitle(stringResource(R.string.settings_section_data))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_title)) },
            supportingContent = { Text(stringResource(R.string.settings_export_summary)) },
            colors = itemColors,
            modifier = Modifier.clickable(enabled = !uiState.isBusy, onClick = onExport),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_import_title)) },
            supportingContent = { Text(stringResource(R.string.settings_import_summary)) },
            colors = itemColors,
            modifier = Modifier.clickable(enabled = !uiState.isBusy, onClick = onImport),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun StepButton(iconRes: Int, description: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(48.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = description)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun SettingsContentPreview() {
    ForkoutTheme {
        SettingsContent(
            uiState = SettingsUiState(amoledBlack = true),
            onAmoledBlackChange = {},
            onStepRest = {},
            onExport = {},
            onImport = {},
        )
    }
}
