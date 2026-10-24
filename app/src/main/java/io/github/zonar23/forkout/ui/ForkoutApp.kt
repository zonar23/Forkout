package io.github.zonar23.forkout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.zonar23.forkout.R
import io.github.zonar23.forkout.ui.components.TimeFormat
import io.github.zonar23.forkout.ui.navigation.ForkoutNavHost
import io.github.zonar23.forkout.ui.navigation.SettingsRoute
import io.github.zonar23.forkout.ui.navigation.TopLevelDestination
import io.github.zonar23.forkout.ui.navigation.WorkoutRoute
import io.github.zonar23.forkout.ui.theme.ForkoutTheme
import io.github.zonar23.forkout.ui.theme.NumberTextStyles
import io.github.zonar23.forkout.ui.workout.currentRest
import io.github.zonar23.forkout.ui.workout.rememberNow
import java.time.Duration

/** App shell: top bar with settings action, bottom navigation and the nav host. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForkoutApp(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    val miniPlayer by viewModel.miniPlayer.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val topLevel = TopLevelDestination.entries.firstOrNull { destination.isOn(it.route) }
    val isSettings = destination.isOn(SettingsRoute)

    Scaffold(
        topBar = {
            // Detail screens such as the exercise editor bring their own top bar.
            if (topLevel == null && !isSettings) return@Scaffold
            CenterAlignedTopAppBar(
                title = {
                    val titleRes = when {
                        isSettings -> R.string.nav_settings
                        topLevel != null -> topLevel.labelRes
                        else -> R.string.app_name
                    }
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    if (isSettings) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.nav_back),
                            )
                        }
                    }
                },
                actions = {
                    if (topLevel != null) {
                        IconButton(
                            onClick = {
                                navController.navigate(SettingsRoute) { launchSingleTop = true }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.nav_settings),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (topLevel != null) {
                Column {
                    miniPlayer?.let { state ->
                        MiniPlayer(
                            state = state,
                            onClick = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                        )
                    }
                    ForkoutNavigationBar(
                        current = topLevel,
                        onSelect = { navController.navigateToTopLevel(it) },
                    )
                }
            }
        },
    ) { innerPadding ->
        ForkoutNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Mini player for a running workout, like in music apps: day, progress and the remaining
 * rest. Tapping it returns to the checklist.
 */
@Composable
private fun MiniPlayer(state: MiniPlayerState, onClick: () -> Unit) {
    val now by rememberNow(intervalMillis = 500)
    val rest = state.sets.currentRest(now, state.skippedRestSetId)
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_timer), contentDescription = null)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.workout_running_banner, state.dayName),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val progress = stringResource(R.string.workout_sets_progress, state.completedSets, state.totalSets)
                    Text(
                        text = rest?.let {
                            val remaining = Duration.between(now, it.endsAt).seconds.coerceAtLeast(0).toInt()
                            "$progress · " + stringResource(R.string.mini_player_rest, TimeFormat.formatMinutesSeconds(remaining))
                        } ?: progress,
                        style = NumberTextStyles.small,
                    )
                }
                Text(stringResource(R.string.workout_resume), style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(
                progress = { if (state.totalSets == 0) 0f else state.completedSets / state.totalSets.toFloat() },
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.primary,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 3.dp),
            )
        }
    }
}

@Composable
private fun ForkoutNavigationBar(
    current: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TopLevelDestination.entries.forEach { item ->
            val selected = item == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    Icon(
                        painter = painterResource(if (selected) item.selectedIconRes else item.iconRes),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(item.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

private fun NavDestination?.isOn(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        // Keep one entry per section and restore its state when coming back.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview
@Composable
private fun ForkoutNavigationBarPreview() {
    ForkoutTheme {
        ForkoutNavigationBar(current = TopLevelDestination.TODAY, onSelect = {})
    }
}
