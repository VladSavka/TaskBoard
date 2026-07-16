package com.example.taskboard.data.repository

import com.example.taskboard.data.datasource.InMemoryTaskDataSource
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.testsupport.FakeTaskDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultTaskRepositoryTest {

    private fun task(id: String, title: String, done: Boolean = false) =
        Task(id = id, title = title, notes = "", priority = Priority.MEDIUM, done = done)

    @Test
    fun `loadTasks returns the remote tasks and caches them locally`() = runTest {
        val remote = FakeTaskDataSource(listOf(task("1", "A"), task("2", "B")))
        val local = InMemoryTaskDataSource()
        val repository = DefaultTaskRepository(remote, local)

        val loaded = repository.loadTasks().first()

        assertEquals(2, loaded.size)
        assertEquals(loaded, local.readTasks())
    }

    @Test
    fun `loadTasks propagates a remote read failure`() = runTest {
        val remote = FakeTaskDataSource().apply { readError = IOException("read failed") }
        val repository = DefaultTaskRepository(remote, InMemoryTaskDataSource())

        val error = runCatching { repository.loadTasks().first() }.exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun `addTask appends and persists to both remote and local`() = runTest {
        val remote = FakeTaskDataSource()
        val local = InMemoryTaskDataSource()
        val repository = DefaultTaskRepository(remote, local)
        repository.loadTasks().first()

        repository.addTask(task("1", "A"))

        assertEquals(listOf("A"), local.readTasks().map { it.title })
        assertEquals(listOf("A"), remote.stored.map { it.title })
    }

    @Test
    fun `updateTask replaces the task with the matching id`() = runTest {
        val remote = FakeTaskDataSource(listOf(task("1", "A"), task("2", "B")))
        val repository = DefaultTaskRepository(remote, InMemoryTaskDataSource())
        repository.loadTasks().first()

        repository.updateTask(task("2", "B2", done = true))

        val stored = remote.stored
        val updated = stored.first { it.id == "2" }
        assertEquals("B2", updated.title)
        assertTrue(updated.done)
        assertEquals("A", stored.first { it.id == "1" }.title)
    }

    @Test
    fun `a failed remote save leaves the local cache and remote untouched`() = runTest {
        val remote = FakeTaskDataSource(listOf(task("1", "A")))
        val local = InMemoryTaskDataSource()
        val repository = DefaultTaskRepository(remote, local)
        repository.loadTasks().first()
        remote.writeError = IOException("mock network save failed")

        val error = runCatching { repository.addTask(task("2", "B")) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(listOf("A"), local.readTasks().map { it.title })
        assertEquals(listOf("A"), remote.stored.map { it.title })
    }

    @Test
    fun `mutations derive from the local cache, not a fresh remote read`() = runTest {
        val remote = FakeTaskDataSource(listOf(task("1", "A")))
        val repository = DefaultTaskRepository(remote, InMemoryTaskDataSource())
        repository.loadTasks().first()
        remote.readError = IOException("remote unavailable")

        repository.addTask(task("2", "B"))

        assertEquals(listOf("A", "B"), remote.stored.map { it.title })
    }

    @Test
    fun `deleteTask removes the task with the matching id`() = runTest {
        val remote = FakeTaskDataSource(listOf(task("1", "A"), task("2", "B")))
        val repository = DefaultTaskRepository(remote, InMemoryTaskDataSource())
        repository.loadTasks().first()

        repository.deleteTask("1")

        assertEquals(listOf("2"), remote.stored.map { it.id })
    }
}
