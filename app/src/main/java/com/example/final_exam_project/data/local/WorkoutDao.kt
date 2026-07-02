package com.example.final_exam_project.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Data access for workout_sessions. getAll/getCountSince are Flow-backed so
// Compose screens observe changes reactively via the ViewModel's StateFlow.
@Dao
interface WorkoutDao {

    // OnConflictStrategy.REPLACE: if a row with the same primary key already exists,
    // delete it and insert the new one. For new sessions (id = 0) Room auto-generates
    // the ID, so conflicts never happen in practice — REPLACE is just a safe default.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE timestamp >= :sinceEpochMillis")
    fun getCountSince(sinceEpochMillis: Long): Flow<Int>
}
