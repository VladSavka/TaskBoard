package com.example.taskboard.data.datasource

import com.example.taskboard.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-lifetime [LocalTaskDataSource]: the in-memory cache. The board has no
 * persistence backend yet, so tasks live in memory for the session, held in a
 * [MutableStateFlow] so [observeTasks] re-emits on every write. Reads always succeed.
 */
class InMemoryTaskDataSource(initial: List<Task> = emptyList()) : LocalTaskDataSource {
    private val tasks = MutableStateFlow(initial)

    override suspend fun readTasks(): List<Task> = tasks.value

    override suspend fun writeTasks(tasks: List<Task>) {
        this.tasks.value = tasks
    }

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()
}
