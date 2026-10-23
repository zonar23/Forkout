package io.github.zonar23.forkout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.zonar23.forkout.ui.exercises.ExerciseEditScreen
import io.github.zonar23.forkout.ui.exercises.ExercisesScreen
import io.github.zonar23.forkout.ui.history.HistoryDetailScreen
import io.github.zonar23.forkout.ui.history.HistoryScreen
import io.github.zonar23.forkout.ui.plans.ExercisePickerScreen
import io.github.zonar23.forkout.ui.plans.PlanDayScreen
import io.github.zonar23.forkout.ui.plans.PlanDetailScreen
import io.github.zonar23.forkout.ui.plans.PlanEditResult
import io.github.zonar23.forkout.ui.plans.PlanEditScreen
import io.github.zonar23.forkout.ui.plans.PlansScreen
import io.github.zonar23.forkout.ui.settings.SettingsScreen
import io.github.zonar23.forkout.ui.today.TodayScreen
import io.github.zonar23.forkout.ui.workout.WorkoutScreen
import io.github.zonar23.forkout.ui.workout.WorkoutSummaryScreen

// Detail screens pop their own route instead of calling popBackStack(), so a second call
// (e.g. back pressed while a save finishes) cannot remove the screen below.

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
        composable<TodayRoute> {
            TodayScreen(
                onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                onEditDay = { dayId -> navController.navigate(PlanDayRoute(dayId)) },
                onOpenWorkoutDetail = { sessionId -> navController.navigate(HistoryDetailRoute(sessionId)) },
                onCreatePlan = { navController.navigate(PlanEditRoute()) },
            )
        }

        composable<PlansRoute> {
            PlansScreen(
                onPlanClick = { id -> navController.navigate(PlanDetailRoute(id)) },
                onAddClick = { navController.navigate(PlanEditRoute()) },
            )
        }
        composable<PlanEditRoute> {
            PlanEditScreen(
                onBack = { navController.popBackStack<PlanEditRoute>(inclusive = true) },
                onResult = { result ->
                    when (result) {
                        is PlanEditResult.Created -> navController.navigate(PlanDetailRoute(result.planId)) {
                            popUpTo<PlanEditRoute> { inclusive = true }
                        }
                        PlanEditResult.Saved, PlanEditResult.NotFound ->
                            navController.popBackStack<PlanEditRoute>(inclusive = true)
                        PlanEditResult.Deleted -> navController.popBackStack<PlansRoute>(inclusive = false)
                    }
                },
            )
        }
        composable<PlanDetailRoute> {
            PlanDetailScreen(
                onBack = { navController.popBackStack<PlanDetailRoute>(inclusive = true) },
                onEditPlan = { id -> navController.navigate(PlanEditRoute(id)) },
                onOpenDay = { id -> navController.navigate(PlanDayRoute(id)) },
                // A copied plan replaces the current detail screen.
                onOpenPlan = { id ->
                    navController.navigate(PlanDetailRoute(id)) {
                        popUpTo<PlanDetailRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<PlanDayRoute> {
            PlanDayScreen(
                onBack = { navController.popBackStack<PlanDayRoute>(inclusive = true) },
                onAddExercise = { dayId -> navController.navigate(ExercisePickerRoute(dayId)) },
                onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
            )
        }
        composable<ExercisePickerRoute> {
            ExercisePickerScreen(onDone = { navController.popBackStack<ExercisePickerRoute>(inclusive = true) })
        }

        composable<WorkoutRoute> {
            WorkoutScreen(
                // Leaving keeps the workout running; it can be resumed from the banner.
                onClose = { navController.popBackStack<WorkoutRoute>(inclusive = true) },
                onFinished = { sessionId ->
                    navController.navigate(WorkoutSummaryRoute(sessionId)) {
                        popUpTo<WorkoutRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<WorkoutSummaryRoute> {
            WorkoutSummaryScreen(onDone = { navController.popBackStack<WorkoutSummaryRoute>(inclusive = true) })
        }

        composable<ExercisesRoute> {
            ExercisesScreen(
                onExerciseClick = { id -> navController.navigate(ExerciseEditRoute(id)) },
                onAddClick = { navController.navigate(ExerciseEditRoute()) },
            )
        }
        composable<ExerciseEditRoute> {
            ExerciseEditScreen(onDone = { navController.popBackStack<ExerciseEditRoute>(inclusive = true) })
        }

        composable<HistoryRoute> {
            HistoryScreen(onWorkoutClick = { sessionId -> navController.navigate(HistoryDetailRoute(sessionId)) })
        }
        composable<HistoryDetailRoute> {
            HistoryDetailScreen(onBack = { navController.popBackStack<HistoryDetailRoute>(inclusive = true) })
        }
        composable<SettingsRoute> { SettingsScreen() }
    }
}
