package com.example.final_exam_project.di

import android.content.Context
import com.example.final_exam_project.data.local.FitTrackDatabase
import com.example.final_exam_project.data.repository.WorkoutRepository

// Manual DI (Dependency Injection) container: one place that creates and holds every
// app-wide object, so nothing outside this file calls constructors directly.
// This project uses manual DI instead of a library like Hilt to keep things simple.
interface AppContainer {
    val workoutRepository: WorkoutRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    // 'by lazy' means the database is only created the first time it is accessed,
    // not when the app starts. This avoids doing disk I/O at startup.
    private val database: FitTrackDatabase by lazy {
        FitTrackDatabase.getInstance(context)
    }

    // Same idea: the repository is created once, on first access, and reused forever.
    // Passing database.workoutDao() wires the repository to the real Room database.
    override val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao())
    }
}
