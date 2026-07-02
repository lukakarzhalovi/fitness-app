package com.example.final_exam_project.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.final_exam_project.data.local.WorkoutSession
import com.example.final_exam_project.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // repository.allSessions is a Room Flow: every INSERT, UPDATE, or DELETE
        // automatically emits a fresh list without any extra code here.
        // viewModelScope is cancelled when the ViewModel is cleared (screen leaves
        // the back stack), so this collector is never leaked.
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
    }

    fun delete(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            // No manual state update needed: Room's Flow re-emits the new list
            // automatically after the DELETE, and the collect block above handles it.
        }
    }
}
