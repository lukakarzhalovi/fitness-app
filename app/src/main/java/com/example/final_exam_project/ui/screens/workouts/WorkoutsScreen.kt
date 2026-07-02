package com.example.final_exam_project.ui.screens.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.final_exam_project.di.ViewModelFactory

@Composable
fun WorkoutsScreen(
    viewModelFactory: ViewModelFactory,
    editingId: Long? = null,
    onSaved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: WorkoutsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Runs once when this screen opens. If editingId is non-null, fetches the
    // matching session from Room and fills the form fields.
    LaunchedEffect(editingId) {
        viewModel.loadForEdit(editingId)
    }

    // Watches the isSaved flag. When save() completes, this block fires once,
    // resets the flag so it won't re-trigger, then calls onSaved() to navigate away.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedHandled()
            onSaved()
        }
    }

    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.exerciseName,
                onValueChange = { viewModel.onExerciseNameChange(it) },
                label = { Text("Exercise name") },
                isError = uiState.exerciseNameError != null,
                // supportingText shows the inline error message below the field,
                // or nothing when the field is valid.
                supportingText = uiState.exerciseNameError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.sets,
                onValueChange = { viewModel.onSetsChange(it) },
                label = { Text("Sets") },
                isError = uiState.setsError != null,
                supportingText = uiState.setsError?.let { error -> { Text(error) } },
                // Number keyboard so the user only sees digits — no letters.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.reps,
                onValueChange = { viewModel.onRepsChange(it) },
                label = { Text("Reps") },
                isError = uiState.repsError != null,
                supportingText = uiState.repsError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.weight,
                onValueChange = { viewModel.onWeightChange(it) },
                label = { Text("Weight (kg)") },
                isError = uiState.weightError != null,
                supportingText = uiState.weightError?.let { error -> { Text(error) } },
                // Decimal keyboard allows the decimal separator for e.g. "72.5".
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Label changes to make it clear whether this is a new entry or an edit.
                Text(if (uiState.editingId != null) "Update workout" else "Save workout")
            }
        }
    }
}
