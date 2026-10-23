package io.github.zonar23.forkout.ui.plans

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
fun PlansScreen(
    modifier: Modifier = Modifier,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlansContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun PlansContent(
    @Suppress("UNUSED_PARAMETER") uiState: PlansUiState,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = R.drawable.ic_list_alt,
        title = stringResource(R.string.nav_plans),
        message = stringResource(R.string.plans_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun PlansContentPreview() {
    ForkoutTheme {
        PlansContent(uiState = PlansUiState())
    }
}
