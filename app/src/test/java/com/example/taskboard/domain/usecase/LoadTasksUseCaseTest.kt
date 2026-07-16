package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LoadTasksUseCaseTest {

    @Test
    fun `emits the tasks from the source`() = runTest {
        val tasks = listOf(Task("1", "A", "", Priority.LOW, done = false))
        val load = LoadTasksUseCase(FakeTaskRepository(tasks))

        assertEquals(tasks, load().first())
    }

    @Test
    fun `propagates a load failure`() = runTest {
        val repository = FakeTaskRepository().apply { loadError = IOException("network down") }
        val load = LoadTasksUseCase(repository)

        val error = runCatching { load().first() }.exceptionOrNull()

        assertTrue("expected an IOException but was $error", error is IOException)
    }
}
