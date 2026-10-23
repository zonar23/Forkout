package io.github.zonar23.forkout.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.ExerciseEntity
import io.github.zonar23.forkout.data.db.model.ExerciseType
import io.github.zonar23.forkout.data.db.model.MuscleGroup
import io.github.zonar23.forkout.ui.components.labelRes
import io.github.zonar23.forkout.ui.theme.ForkoutTheme

@Composable
fun ExercisesScreen(
    onExerciseClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisesContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onTypeToggle = viewModel::onTypeToggle,
        onMuscleGroupToggle = viewModel::onMuscleGroupToggle,
        onShowArchivedToggle = viewModel::onShowArchivedToggle,
        onExerciseClick = onExerciseClick,
        onAddClick = onAddClick,
        modifier = modifier,
    )
}

@Composable
private fun ExercisesContent(
    uiState: ExercisesUiState,
    onQueryChange: (String) -> Unit,
    onTypeToggle: (ExerciseType) -> Unit,
    onMuscleGroupToggle: (MuscleGroup) -> Unit,
    onShowArchivedToggle: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchField(
                query = uiState.filter.query,
                onQueryChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilterRow {
                items(ExerciseType.entries) { type ->
                    FilterChip(
                        selected = uiState.filter.type == type,
                        onClick = { onTypeToggle(type) },
                        label = { Text(stringResource(type.labelRes)) },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filter.showArchived,
                        onClick = onShowArchivedToggle,
                        label = { Text(stringResource(R.string.exercises_show_archived)) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_archive), contentDescription = null)
                        },
                    )
                }
            }
            FilterRow {
                items(MuscleGroup.entries) { group ->
                    FilterChip(
                        selected = uiState.filter.muscleGroup == group,
                        onClick = { onMuscleGroupToggle(group) },
                        label = { Text(stringResource(group.labelRes)) },
                    )
                }
            }

            if (!uiState.isLoading && uiState.exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.exercises_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    // Leaves room so the FAB never covers the last item.
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.exercises_add),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.exercises_search)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.exercises_clear_search),
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun FilterRow(content: LazyListScope.() -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        content = content,
    )
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
) {
    val details = buildList {
        add(stringResource(exercise.type.labelRes))
        if (exercise.muscleGroups.isNotEmpty()) {
            add(
                exercise.muscleGroups
                    .sortedBy { it.ordinal }
                    .map { stringResource(it.labelRes) }
                    .joinToString(", ")
            )
        }
        if (exercise.isArchived) add(stringResource(R.string.exercises_archived_label))
    }.joinToString(" · ")

    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = { Text(details) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .clickable(onClick = onClick)
            .alpha(if (exercise.isArchived) 0.5f else 1f),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun ExercisesContentPreview() {
    ForkoutTheme {
        ExercisesContent(
            uiState = ExercisesUiState(
                exercises = listOf(
                    ExerciseEntity(1, "Beinpresse", ExerciseType.MACHINE, setOf(MuscleGroup.LEGS, MuscleGroup.GLUTES)),
                    ExerciseEntity(2, "Latzug", ExerciseType.MACHINE, setOf(MuscleGroup.BACK), isArchived = true),
                ),
                isLoading = false,
            ),
            onQueryChange = {},
            onTypeToggle = {},
            onMuscleGroupToggle = {},
            onShowArchivedToggle = {},
            onExerciseClick = {},
            onAddClick = {},
        )
    }
}
