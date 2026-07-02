package com.example.final_exam_project.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.final_exam_project.data.local.WorkoutSession
import com.example.final_exam_project.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutsViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutFormUiState())
    val uiState: StateFlow<WorkoutFormUiState> = _uiState.asStateFlow()

    // Clear the error for the field the user is actively editing so red text
    // disappears as soon as they start typing a correction.
    fun onExerciseNameChange(value: String) =
        _uiState.update { it.copy(exerciseName = value, exerciseNameError = null) }

    fun onSetsChange(value: String) =
        _uiState.update { it.copy(sets = value, setsError = null) }

    fun onRepsChange(value: String) =
        _uiState.update { it.copy(reps = value, repsError = null) }

    fun onWeightChange(value: String) =
        _uiState.update { it.copy(weight = value, weightError = null) }

    // Called when the user taps "Edit" on a History row. Fetches the session from
    // Room and pre-populates every form field (numbers are converted to strings
    // because OutlinedTextField always works with text, not numbers).
    fun loadForEdit(id: Long?) {
        if (id == null) return
        viewModelScope.launch {
            val session = repository.getSessionById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    editingId = id,
                    exerciseName = session.exerciseName,
                    sets = session.sets.toString(),
                    reps = session.reps.toString(),
                    weight = session.weight.toString()
                )
            }
        }
    }

    // Validates all fields, then either inserts a new session or updates the
    // existing one (depending on whether editingId is set). On success sets
    // isSaved = true so the screen knows it can navigate away.
    fun save() {
        val state = _uiState.value
        var updated = state
        var hasError = false

        if (state.exerciseName.isBlank()) {
            updated = updated.copy(exerciseNameError = "Exercise name is required")
            hasError = true
        }

        // toIntOrNull returns null for blank, letters, or decimals — all invalid here.
        val setsInt = state.sets.toIntOrNull()
        if (setsInt == null || setsInt <= 0) {
            updated = updated.copy(setsError = "Enter a positive whole number")
            hasError = true
        }

        val repsInt = state.reps.toIntOrNull()
        if (repsInt == null || repsInt <= 0) {
            updated = updated.copy(repsError = "Enter a positive whole number")
            hasError = true
        }

        // Weight can be 0 (e.g. bodyweight exercise), but not negative or non-numeric.
        val weightDouble = state.weight.toDoubleOrNull()
        if (weightDouble == null || weightDouble < 0.0) {
            updated = updated.copy(weightError = "Enter a valid weight (0 or more)")
            hasError = true
        }

        if (hasError) {
            _uiState.value = updated
            return
        }

        viewModelScope.launch {
            val editingId = state.editingId
            if (editingId != null) {
                // Keep the original timestamp so history order is unchanged after an edit.
                val existing = repository.getSessionById(editingId)
                if (existing != null) {
                    repository.updateSession(
                        existing.copy(
                            exerciseName = state.exerciseName.trim(),
                            sets = setsInt!!,
                            reps = repsInt!!,
                            weight = weightDouble!!
                        )
                    )
                }
            } else {
                repository.addSession(
                    WorkoutSession(
                        exerciseName = state.exerciseName.trim(),
                        sets = setsInt!!,
                        reps = repsInt!!,
                        weight = weightDouble!!
                    )
                )
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    // Called by the screen right after it reacts to isSaved = true, so the flag
    // doesn't fire again if the same ViewModel instance is reused on back-navigation.
    fun onSavedHandled() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
