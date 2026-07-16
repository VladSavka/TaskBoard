package com.example.taskboard.domain.repository

import com.example.taskboard.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and mutating the task collection. Implementations live in
 * the data layer. Mutations write through the cache and don't return anything —
 * observers of [loadTasks] (and [task]) see the change.
 */
interface TaskRepository {
    /**
     * Observes the task collection. Collecting fetches from the remote source (may
     * throw when it fails — this drives the loading/error/retry behavior), then
     * re-emits a fresh list after every mutation, so callers just collect and never
     * need a separate cache read.
     */
    fun loadTasks(): Flow<List<Task>>

    /** Observes a single task by [id] from the cache, re-emitting on change; null when it is gone. */
    fun task(id: String): Flow<Task?>

    /** Adds [task]. Observers of [loadTasks] see the new collection. */
    suspend fun addTask(task: Task)

    /** Replaces the task sharing [Task.id] with [task]. Observers see the change. */
    suspend fun updateTask(task: Task)

    /** Removes the task with [taskId]. Observers see the change. */
    suspend fun deleteTask(taskId: String)
}
