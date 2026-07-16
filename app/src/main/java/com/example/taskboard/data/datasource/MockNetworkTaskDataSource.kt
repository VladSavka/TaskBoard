package com.example.taskboard.data.datasource

import com.example.taskboard.domain.model.Task
import java.io.IOException

/**
 * [RemoteTaskDataSource] standing in for a real backend. Every call waits out a
 * simulated [latency] and may fail per the [failurePolicy], so the app's
 * loading and error states are exercised for real. A failed [writeTasks] leaves
 * the stored collection untouched (the change is not persisted).
 *
 * Latency and failure are injected so the behavior is deterministic under test.
 */
class MockNetworkTaskDataSource(
    initial: List<Task> = emptyList(),
    private val failurePolicy: NetworkFailurePolicy,
    private val latency: NetworkLatency,
) : RemoteTaskDataSource {
    private var stored: List<Task> = initial

    override suspend fun readTasks(): List<Task> {
        latency.await()
        if (failurePolicy.shouldFail()) throw IOException("mock network read failed")
        return stored
    }

    override suspend fun writeTasks(tasks: List<Task>) {
        latency.await()
        if (failurePolicy.shouldFail()) throw IOException("mock network save failed")
        stored = tasks
    }
}
