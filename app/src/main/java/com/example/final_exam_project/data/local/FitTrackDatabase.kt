package com.example.final_exam_project.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkoutSession::class], version = 1, exportSchema = false)
abstract class FitTrackDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        // @Volatile makes writes to INSTANCE immediately visible to all threads,
        // preventing a thread from reading a stale null after another thread wrote the value.
        @Volatile private var INSTANCE: FitTrackDatabase? = null

        fun getInstance(context: Context): FitTrackDatabase =
            // Double-checked locking pattern:
            // 1st check (outside synchronized): avoids the lock on every call once the DB exists.
            // 2nd check (inside synchronized): guards against two threads both passing the 1st
            //   check at the same time and each trying to create the database.
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,  // use applicationContext so the DB outlives any single Activity
                    FitTrackDatabase::class.java,
                    "fittrack.db"
                ).build().also { INSTANCE = it }  // store the new instance before returning it
            }
    }
}
