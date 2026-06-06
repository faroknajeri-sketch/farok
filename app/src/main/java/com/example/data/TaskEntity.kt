package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val time: String, // Readable time display (e.g. 10:30 AM)
    val timeInMillis: Long, // Exact alarm time in millis
    val isDone: Boolean = false,
    val category: String = "General", // General, Work, Personal, Health, Shopping etc.
    val priority: String = "Medium" // Low, Medium, High
)
