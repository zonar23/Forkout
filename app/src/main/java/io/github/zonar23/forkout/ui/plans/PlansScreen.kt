package io.github.zonar23.forkout.ui.plans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.data.db.entity.PlanEntity
import io.github.zonar23.forkout.ui.components.PlaceholderContent
import io.github.zonar23.forkout.ui.theme.ForkoutTheme
import java.time.Instant

@Composable
fun PlansScreen(
    onPlanClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlansViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlansContent(uiState = uiState, onPlanClick = onPlanClick, onAddClick = onAddClick, modifier = modifier)
}

@Composable
private fun PlansContent(
    uiState: PlansUiState,
    onPlanClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isLoading && uiState.plans.isEmpty()) {
            PlaceholderContent(
                iconRes = R.drawable.ic_list_alt,
                title = stringResource(R.string.nav_plans),
                message = stringResource(R.string.plans_empty),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.plans, key = { it.id }) { plan ->
                    ListItem(
                        headlineContent = { Text(plan.name) },
                        supportingContent = { Text(plan.summary()) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.clickable { onPlanClick(plan.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
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
            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.plans_add))
        }
    }
}

/** "3× pro Woche · Muskelaufbau" */
@Composable
internal fun PlanEntity.summary(): String = listOfNotNull(
    stringResource(R.string.plans_sessions_per_week, sessionsPerWeek),
    goal.takeIf { it.isNotBlank() },
).joinToString(" · ")

@Preview(showBackground = true, backgroundColor = 0xFF0E0F12)
@Composable
private fun PlansContentPreview() {
    ForkoutTheme {
        PlansContent(
            uiState = PlansUiState(
                plans = listOf(
                    PlanEntity(1, "Coach-Plan Herbst 2026", "Muskelaufbau", 3, createdAt = Instant.EPOCH),
                ),
                isLoading = false,
            ),
            onPlanClick = {},
            onAddClick = {},
        )
    }
}
