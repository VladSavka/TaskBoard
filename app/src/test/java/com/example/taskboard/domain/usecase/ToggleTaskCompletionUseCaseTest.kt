package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleTaskCompletionUseCaseTest {

    private fun taskOf(id: String, done: Boolean) =
        Task(id = id, title = "Book dentist", notes = "", priority = Priority.LOW, done = done)

    @Test
    fun `marks an incomplete task complete`() = runTest {
        val repository = FakeTaskRepository(listOf(taskOf("1", done = false)))
        val toggle = ToggleTaskCompletionUseCase(repository)

        toggle(taskOf("1", done = false))

        assertTrue(repository.getTasks().single().done)
    }

    @Test
    fun `marks a complete task incomplete`() = runTest {
        val repository = FakeTaskRepository(listOf(taskOf("1", done = true)))
        val toggle = ToggleTaskCompletionUseCase(repository)

        toggle(taskOf("1", done = true))

        assertFalse(repository.getTasks().single().done)
    }

    @Test
    fun `a task not in the store leaves the board unchanged`() = runTest {
        val repository = FakeTaskRepository(listOf(taskOf("1", done = false)))
        val toggle = ToggleTaskCompletionUseCase(repository)

        toggle(taskOf("missing", done = false))

        val tasks = repository.getTasks()
        assertEquals(1, tasks.size)
        assertFalse(tasks.single().done)
    }
}
