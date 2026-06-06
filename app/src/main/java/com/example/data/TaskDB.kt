package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class TaskDB : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        @Volatile
        private var instance: TaskDB? = null

        fun get(context: Context): TaskDB {
            return instance ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDB::class.java,
                    "task_db"
                )
                .fallbackToDestructiveMigration() // Gracefully recreate DB if version increments
                .build()
                instance = db
                db
            }
        }
    }
}
