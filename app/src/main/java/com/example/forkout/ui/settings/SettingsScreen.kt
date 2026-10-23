package com.example.forkout.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forkout.R
import com.example.forkout.ui.components.PlaceholderContent
import com.example.forkout.ui.theme.ForkoutTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun SettingsContent(
    @Suppress("UNUSED_PARAMETER") uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = R.drawable.ic_settings,
        title = stringResource(R.string.nav_settings),
        message = stringResource(R.string.settings_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun SettingsContentPreview() {
    ForkoutTheme {
        SettingsContent(uiState = SettingsUiState())
    }
}
