package io.github.zonar23.forkout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.zonar23.forkout.ui.exercises.ExerciseEditScreen
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
        composable<ExercisesRoute> {
            ExercisesScreen(
                onExerciseClick = { id -> navController.navigate(ExerciseEditRoute(id)) },
                onAddClick = { navController.navigate(ExerciseEditRoute()) },
            )
        }
        composable<ExerciseEditRoute> {
            // Pops only the editor, so a second call (back + save) cannot leave the list.
            ExerciseEditScreen(onDone = { navController.popBackStack<ExerciseEditRoute>(inclusive = true) })
        }
        composable<HistoryRoute> { HistoryScreen() }
        composable<SettingsRoute> { SettingsScreen() }
    }
}
