package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteRestoreUpdateUseCaseTest {

    private val kept = Task("1", "Reply to design feedback", "", Priority.MEDIUM, done = false)
    private val target = Task("2", "Renew domain registration", "note", Priority.HIGH, done = false)

    @Test
    fun `delete removes only the named task`() = runTest {
        val repository = FakeTaskRepository(listOf(kept, target))
        val delete = DeleteTaskUseCase(repository)

        delete("2")

        assertEquals(listOf(kept), repository.getTasks())
    }

    @Test
    fun `restore puts a deleted task back with the same identity`() = runTest {
        val repository = FakeTaskRepository(listOf(kept))
        val restore = RestoreTaskUseCase(repository)

        restore(target)

        val tasks = repository.getTasks()
        assertTrue(tasks.contains(target))
        assertEquals("2", tasks.first { it.title == "Renew domain registration" }.id)
    }

    @Test
    fun `update replaces the matching task's fields, keeping its identity`() = runTest {
        val repository = FakeTaskRepository(listOf(target))
        val update = UpdateTaskUseCase(repository)

        val edited = target.copy(title = "Renew SSL certificate", notes = "Expires next week", priority = Priority.LOW)
        update(edited)

        val saved = repository.getTasks().single()
        assertEquals("2", saved.id)
        assertEquals("Renew SSL certificate", saved.title)
        assertEquals("Expires next week", saved.notes)
        assertEquals(Priority.LOW, saved.priority)
        assertFalse(saved.done)
    }
}
