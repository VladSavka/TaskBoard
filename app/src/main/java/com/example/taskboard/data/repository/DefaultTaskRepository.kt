package com.example.taskboard.data.repository

import com.example.taskboard.data.datasource.LocalTaskDataSource
import com.example.taskboard.data.datasource.RemoteTaskDataSource
import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * [TaskRepository] over a [remote] source (the network) and a [local] cache that is
 * the single source of truth. Collecting [loadTasks] fetches from the remote once,
 * writes it into the cache, then streams the cache — so every mutation re-emits.
 *
 * Mutations are optimistic: the cache updates immediately (the UI sees the change
 * at once), then the write goes to the remote; if that fails the cache is reverted
 * to its previous value and the error is rethrown, so the UI can roll back.
 */
class DefaultTaskRepository(
    private val remote: RemoteTaskDataSource,
    private val local: LocalTaskDataSource,
) : TaskRepository {

    override fun loadTasks(): Flow<List<Task>> = flow {
        val tasks = remote.readTasks()
        local.writeTasks(tasks)
        emitAll(local.observeTasks())
    }

    override fun task(id: String): Flow<Task?> =
        local.observeTasks().map { tasks -> tasks.firstOrNull { it.id == id } }

    override suspend fun addTask(task: Task) = commit(local.readTasks() + task)

    override suspend fun updateTask(task: Task) =
        commit(local.readTasks().map { if (it.id == task.id) task else it })

    override suspend fun deleteTask(taskId: String) =
        commit(local.readTasks().filterNot { it.id == taskId })

    private suspend fun commit(next: List<Task>) {
        val previous = local.readTasks()
        local.writeTasks(next) // optimistic: the cache flow emits right away
        try {
            remote.writeTasks(next)
        } catch (e: Throwable) {
            local.writeTasks(previous) // revert on a failed remote write
            throw e
        }
    }
}
