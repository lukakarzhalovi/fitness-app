package com.example.final_exam_project.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.final_exam_project.ui.screens.dashboard.DashboardViewModel
import com.example.final_exam_project.ui.screens.history.HistoryViewModel
import com.example.final_exam_project.ui.screens.workouts.WorkoutsViewModel

// ViewModelProvider.Factory tells Android how to construct ViewModels that need
// constructor arguments (our repository). Without this, Android can only create
// ViewModels that have a zero-argument constructor.
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    // The default create() returns T but we must cast manually, hence the suppression.
    // isAssignableFrom checks "is modelClass the same as, or a subclass of, XViewModel?"
    // — this handles subclassing safely instead of a fragile equality check.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(container.workoutRepository) as T

        modelClass.isAssignableFrom(WorkoutsViewModel::class.java) ->
            WorkoutsViewModel(container.workoutRepository) as T

        modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
            HistoryViewModel(container.workoutRepository) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
