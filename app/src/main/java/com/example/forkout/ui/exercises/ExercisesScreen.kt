package com.example.forkout.ui.exercises

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
fun ExercisesScreen(
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun ExercisesContent(
    @Suppress("UNUSED_PARAMETER") uiState: ExercisesUiState,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = R.drawable.ic_fitness_center,
        title = stringResource(R.string.nav_exercises),
        message = stringResource(R.string.exercises_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun ExercisesContentPreview() {
    ForkoutTheme {
        ExercisesContent(uiState = ExercisesUiState())
    }
}
