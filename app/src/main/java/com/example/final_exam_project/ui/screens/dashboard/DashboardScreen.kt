package com.example.final_exam_project.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.final_exam_project.di.ViewModelFactory
import com.example.final_exam_project.ui.components.GoalRing
import com.example.final_exam_project.ui.components.StatCard

@Composable
fun DashboardScreen(
    viewModelFactory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    val viewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weekly Goal",
                style = MaterialTheme.typography.titleLarge
            )

            GoalRing(progress = uiState.goalProgress)

            // Context label so the number makes sense without the user having
            // to interpret the percentage alone.
            Text(
                text = "${uiState.sessionsThisWeek} / ${uiState.weeklyGoalTarget} workouts this week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total sessions",
                    value = uiState.totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Streak",
                    value = "${uiState.currentStreak}d",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
