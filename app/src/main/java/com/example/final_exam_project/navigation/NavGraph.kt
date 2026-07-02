package com.example.final_exam_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.final_exam_project.di.ViewModelFactory
import com.example.final_exam_project.ui.screens.dashboard.DashboardScreen
import com.example.final_exam_project.ui.screens.history.HistoryScreen
import com.example.final_exam_project.ui.screens.workouts.WorkoutsScreen

private const val EDIT_ID_ARG = "editId"

@Composable
fun FitTrackNavGraph(
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Dashboard.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Dashboard.route) {
            DashboardScreen(viewModelFactory = viewModelFactory)
        }

        // "New workout" route (no ID in the URL): after saving, switch to History
        // so the user can immediately see the entry they just logged.
        composable(BottomNavItem.Workouts.route) {
            WorkoutsScreen(
                viewModelFactory = viewModelFactory,
                onSaved = {
                    // Same popUpTo/launchSingleTop/restoreState combo as the bottom
                    // bar — clears the back stack and restores the History tab's state.
                    navController.navigate(BottomNavItem.History.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // "Edit workout" route: the workout's database ID is embedded in the URL
        // (e.g. "workouts/42") so the form knows which record to load.
        // navArgument declares the type as LongType so Navigation parses it automatically.
        composable(
            route = "${BottomNavItem.Workouts.route}/{$EDIT_ID_ARG}",
            arguments = listOf(navArgument(EDIT_ID_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            // Read the ID that Navigation extracted from the URL segment.
            val editingId = backStackEntry.arguments?.getLong(EDIT_ID_ARG)
            WorkoutsScreen(
                viewModelFactory = viewModelFactory,
                editingId = editingId,
                // popBackStack() removes this edit screen and returns to History,
                // which is simpler than navigating to History explicitly from here.
                onSaved = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.History.route) {
            HistoryScreen(
                viewModelFactory = viewModelFactory,
                onEditSession = { id -> navController.navigate("${BottomNavItem.Workouts.route}/$id") }
            )
        }
    }
}
