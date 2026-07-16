package com.example.taskboard.testsupport

import com.example.taskboard.data.datasource.RemoteTaskDataSource
import com.example.taskboard.domain.model.Task

/**
 * Controllable [RemoteTaskDataSource] for unit tests. [readError], when set, makes
 * [readTasks] fail; [writeError], when set, makes [writeTasks] fail without
 * changing [stored]; otherwise it returns the last written (or seeded) tasks.
 */
class FakeTaskDataSource(initial: List<Task> = emptyList()) : RemoteTaskDataSource {
    var stored: List<Task> = initial
    var readError: Throwable? = null
    var writeError: Throwable? = null

    override suspend fun readTasks(): List<Task> {
        readError?.let { throw it }
        return stored
    }

    override suspend fun writeTasks(tasks: List<Task>) {
        writeError?.let { throw it }
        stored = tasks
    }
}
