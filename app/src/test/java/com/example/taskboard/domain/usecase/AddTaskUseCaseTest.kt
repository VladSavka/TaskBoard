package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AddTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val addTask = AddTaskUseCase(repository) { "generated-id" }

    @Test
    fun `saves an incomplete task with the given fields`() = runTest {
        val due = LocalDate.of(2026, 8, 1)
        addTask("Renew domain registration", "Expires end of month", Priority.HIGH, due)

        val saved = repository.getTasks().single()
        assertEquals("Renew domain registration", saved.title)
        assertEquals("Expires end of month", saved.notes)
        assertEquals(Priority.HIGH, saved.priority)
        assertEquals(due, saved.dueDate)
        assertFalse(saved.done)
        assertEquals("generated-id", saved.id)
    }

    @Test
    fun `does not save a task with a blank title`() = runTest {
        addTask("", "notes without a title", Priority.MEDIUM, null)

        assertTrue(repository.getTasks().isEmpty())
    }

    @Test
    fun `treats a whitespace-only title as blank`() = runTest {
        addTask("   ", "notes", Priority.LOW, null)

        assertTrue(repository.getTasks().isEmpty())
    }
}
