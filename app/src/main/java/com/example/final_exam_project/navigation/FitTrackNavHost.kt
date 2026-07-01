package com.example.final_exam_project.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.final_exam_project.di.ViewModelFactory

// Top-level Scaffold: bottom NavigationBar (Dashboard/Workouts/History) driving the NavHost.
@Composable
fun FitTrackNavHost(viewModelFactory: ViewModelFactory) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination

            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        FitTrackNavGraph(
            navController = navController,
            viewModelFactory = viewModelFactory,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
