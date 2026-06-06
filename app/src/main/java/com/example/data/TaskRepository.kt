package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = dao.getAllFlow()

    suspend fun getTaskById(id: Int): TaskEntity? = dao.getTaskById(id)

    suspend fun add(task: TaskEntity): Long = dao.insert(task)

    suspend fun update(task: TaskEntity) = dao.update(task)

    suspend fun delete(task: TaskEntity) = dao.delete(task)
}
