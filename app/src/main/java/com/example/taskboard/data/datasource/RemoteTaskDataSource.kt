package com.example.taskboard.data.datasource

import com.example.taskboard.domain.model.Task

/**
 * The remote task store — the app's (simulated) network backend. Reads and writes
 * go over the "wire", so they take time and can fail; the repository fetches from
 * here and mirrors the result into a [LocalTaskDataSource] cache. [readTasks] may
 * throw; [writeTasks] persists the full collection.
 */
interface RemoteTaskDataSource {
    suspend fun readTasks(): List<Task>
    suspend fun writeTasks(tasks: List<Task>)
}
