package io.github.zonar23.forkout.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.PlaceholderContent
import io.github.zonar23.forkout.ui.theme.ForkoutTheme

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun HistoryContent(
    @Suppress("UNUSED_PARAMETER") uiState: HistoryUiState,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = R.drawable.ic_history,
        title = stringResource(R.string.nav_history),
        message = stringResource(R.string.history_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun HistoryContentPreview() {
    ForkoutTheme {
        HistoryContent(uiState = HistoryUiState())
    }
}
