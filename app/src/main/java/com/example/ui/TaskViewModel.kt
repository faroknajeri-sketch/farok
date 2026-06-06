package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            val insertedId = repository.add(task)
            // Schedule the alarm if the task is not done and the schedule time is in the future
            if (!task.isDone && task.timeInMillis > System.currentTimeMillis()) {
                AlarmReceiver.setAlarm(
                    getApplication(),
                    task.timeInMillis,
                    task.title,
                    insertedId.toInt()
                )
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task)
            if (task.isDone) {
                // Cancel scheduled alarms if marked complete
                AlarmReceiver.cancelAlarm(getApplication(), task.id)
            } else {
                // Reschedule alarm for updated time / title
                if (task.timeInMillis > System.currentTimeMillis()) {
                    AlarmReceiver.setAlarm(
                        getApplication(),
                        task.timeInMillis,
                        task.title,
                        task.id
                    )
                } else {
                    AlarmReceiver.cancelAlarm(getApplication(), task.id)
                }
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity, isDone: Boolean) {
        viewModelScope.launch {
            val updated = task.copy(isDone = isDone)
            repository.update(updated)
            if (isDone) {
                AlarmReceiver.cancelAlarm(getApplication(), task.id)
            } else if (task.timeInMillis > System.currentTimeMillis()) {
                AlarmReceiver.setAlarm(
                    getApplication(),
                    task.timeInMillis,
                    task.title,
                    task.id
                )
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.delete(task)
            AlarmReceiver.cancelAlarm(getApplication(), task.id)
        }
    }
}

class TaskViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
