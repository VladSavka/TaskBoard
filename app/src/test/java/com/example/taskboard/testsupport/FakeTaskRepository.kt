package com.example.taskboard.testsupport

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * In-memory reactive [TaskRepository] for unit tests. [loadError], when set, makes
 * the next [loadTasks] collection fail; [failLoadOnce] fails a single collection
 * and then recovers; [loadGate], when set, suspends [loadTasks] until it completes
 * (an in-flight read). [saveError], when set, makes the next mutating call fail —
 * the change is applied optimistically then reverted, mirroring the real repository.
 */
class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {
    private val cache = MutableStateFlow(initial)
    var loadError: Throwable? = null
    var failLoadOnce: Boolean = false
    var loadGate: CompletableDeferred<Unit>? = null
    var saveError: Throwable? = null

    override fun loadTasks(): Flow<List<Task>> = flow {
        loadGate?.await()
        if (failLoadOnce) {
            failLoadOnce = false
            throw IllegalStateException("scripted one-time load failure")
        }
        loadError?.let { throw it }
        emitAll(cache)
    }

    override fun task(id: String): Flow<Task?> =
        cache.map { tasks -> tasks.firstOrNull { it.id == id } }

    /** Test-only snapshot of the stored tasks (the interface has no cache-read method). */
    suspend fun getTasks(): List<Task> = cache.value

    override suspend fun addTask(task: Task) = commit(cache.value + task)

    override suspend fun updateTask(task: Task) =
        commit(cache.value.map { if (it.id == task.id) task else it })

    override suspend fun deleteTask(taskId: String) =
        commit(cache.value.filterNot { it.id == taskId })

    private fun commit(next: List<Task>) {
        val previous = cache.value
        cache.value = next
        saveError?.let {
            cache.value = previous
            throw it
        }
    }
}
