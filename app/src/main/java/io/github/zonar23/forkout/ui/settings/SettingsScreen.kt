package io.github.zonar23.forkout.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.theme.ForkoutTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onAmoledBlackChange = viewModel::setAmoledBlack,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onAmoledBlackChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_amoled_title)) },
            supportingContent = { Text(stringResource(R.string.settings_amoled_summary)) },
            trailingContent = {
                // The whole row toggles; the switch only shows the state.
                Switch(checked = uiState.amoledBlack, onCheckedChange = null)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.toggleable(
                value = uiState.amoledBlack,
                role = Role.Switch,
                onValueChange = onAmoledBlackChange,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun SettingsContentPreview() {
    ForkoutTheme {
        SettingsContent(uiState = SettingsUiState(amoledBlack = true), onAmoledBlackChange = {})
    }
}
