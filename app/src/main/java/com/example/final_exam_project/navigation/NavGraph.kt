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

        // "New workout" route: after saving, switch to the History tab so the user
        // can immediately see the entry they just logged.
        composable(BottomNavItem.Workouts.route) {
            WorkoutsScreen(
                viewModelFactory = viewModelFactory,
                onSaved = {
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

        // "Edit workout" route: after saving, pop this screen off the back stack
        // to return to the History screen the user came from.
        composable(
            route = "${BottomNavItem.Workouts.route}/{$EDIT_ID_ARG}",
            arguments = listOf(navArgument(EDIT_ID_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val editingId = backStackEntry.arguments?.getLong(EDIT_ID_ARG)
            WorkoutsScreen(
                viewModelFactory = viewModelFactory,
                editingId = editingId,
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
