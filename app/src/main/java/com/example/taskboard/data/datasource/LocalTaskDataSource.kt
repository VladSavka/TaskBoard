package com.example.taskboard.data.datasource

import com.example.taskboard.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * The local task store — the in-memory cache and single source of truth the
 * repository serves from. It holds the last collection fetched from or written
 * through to the [RemoteTaskDataSource] and exposes it via [observeTasks], which
 * re-emits on every write. Reads are immediate and never fail.
 */
interface LocalTaskDataSource {
    suspend fun readTasks(): List<Task>
    suspend fun writeTasks(tasks: List<Task>)

    /** Observes the cached tasks, emitting the current value and every subsequent write. */
    fun observeTasks(): Flow<List<Task>>
}
