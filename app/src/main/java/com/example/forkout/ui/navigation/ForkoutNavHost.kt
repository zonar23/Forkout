package com.example.forkout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.forkout.ui.exercises.ExercisesScreen
import com.example.forkout.ui.history.HistoryScreen
import com.example.forkout.ui.plans.PlansScreen
import com.example.forkout.ui.settings.SettingsScreen
import com.example.forkout.ui.today.TodayScreen

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
