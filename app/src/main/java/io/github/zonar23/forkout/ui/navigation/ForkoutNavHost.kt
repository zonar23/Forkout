package io.github.zonar23.forkout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.zonar23.forkout.ui.exercises.ExercisesScreen
import io.github.zonar23.forkout.ui.history.HistoryScreen
import io.github.zonar23.forkout.ui.plans.PlansScreen
import io.github.zonar23.forkout.ui.settings.SettingsScreen
import io.github.zonar23.forkout.ui.today.TodayScreen

@Composable
fun ForkoutNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TodayRoute,
        modifier = modifier,
    ) {
        composable<TodayRoute> { TodayScreen() }
        composable<PlansRoute> { PlansScreen() }
        composable<ExercisesRoute> { ExercisesScreen() }
        composable<HistoryRoute> { HistoryScreen() }
        composable<SettingsRoute> { SettingsScreen() }
    }
}
