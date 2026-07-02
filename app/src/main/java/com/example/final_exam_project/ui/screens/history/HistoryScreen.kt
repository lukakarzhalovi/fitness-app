package com.example.final_exam_project.ui.screens.history

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.final_exam_project.di.ViewModelFactory
import com.example.final_exam_project.ui.components.EmptyState
import com.example.final_exam_project.ui.components.WorkoutListItem

@Composable
fun HistoryScreen(
    viewModelFactory: ViewModelFactory,
    onEditSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { paddingValues ->
        when {
            // isLoading is true until the first Room emission arrives; showing nothing
            // here prevents a false "no workouts yet" flash on startup.
            uiState.isLoading -> Unit

            uiState.sessions.isEmpty() ->
                EmptyState(message = "No workouts logged yet", modifier = Modifier)

            else ->
                LazyColumn(modifier = Modifier.padding(paddingValues)) {
                    items(uiState.sessions, key = { it.id }) { session ->
                        WorkoutListItem(
                            session = session,
                            onClick = { onEditSession(it.id) },
                            onDelete = { viewModel.delete(it) }
                        )
                    }
                }
        }
    }
}
