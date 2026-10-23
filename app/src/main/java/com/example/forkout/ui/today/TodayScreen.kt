package com.example.forkout.ui.today

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
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun TodayContent(
    @Suppress("UNUSED_PARAMETER") uiState: TodayUiState,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = R.drawable.ic_today,
        title = stringResource(R.string.nav_today),
        message = stringResource(R.string.today_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun TodayContentPreview() {
    ForkoutTheme {
        TodayContent(uiState = TodayUiState())
    }
}
