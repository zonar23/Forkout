package com.example.forkout.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.forkout.R

/** The four sections reachable from the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int,
) {
    TODAY(TodayRoute, R.string.nav_today, R.drawable.ic_today, R.drawable.ic_today_filled),
    PLANS(PlansRoute, R.string.nav_plans, R.drawable.ic_list_alt, R.drawable.ic_list_alt_filled),
    EXERCISES(
        ExercisesRoute,
        R.string.nav_exercises,
        R.drawable.ic_fitness_center,
        R.drawable.ic_fitness_center_filled,
    ),
    HISTORY(HistoryRoute, R.string.nav_history, R.drawable.ic_history, R.drawable.ic_history_filled),
}
