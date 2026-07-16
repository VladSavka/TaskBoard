package com.example.taskboard.presentation.taskdetail

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.usecase.GetTaskUseCase
import com.example.taskboard.domain.usecase.UpdateTaskUseCase
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(taskId: String, repository: FakeTaskRepository): TaskDetailViewModel =
        TaskDetailViewModel(taskId, GetTaskUseCase(repository), UpdateTaskUseCase(repository))

    private fun task(
        title: String,
        notes: String = "",
        priority: Priority = Priority.MEDIUM,
        dueDate: LocalDate? = null,
    ) = Task(id = title, title = title, notes = notes, priority = priority, dueDate = dueDate)

    @Test
    fun `an unknown task id leaves the detail empty`() {
        assertNull(viewModelFor("ghost", FakeTaskRepository()).uiState.value)
    }

    @Test
    fun `constructing loads the task by id and seeds the editable fields`() {
        val due = LocalDate.of(2026, 7, 20)
        val stored = task("Book dentist", notes = "Annual checkup", priority = Priority.LOW, dueDate = due)
        val viewModel = viewModelFor("Book dentist", FakeTaskRepository(listOf(stored)))

        assertEquals(TaskDetail("Book dentist", "Annual checkup", Priority.LOW, due), viewModel.uiState.value)
    }

    @Test
    fun `editing updates the observable fields including the due date`() {
        val viewModel = viewModelFor("Book dentist", FakeTaskRepository(listOf(task("Book dentist"))))
        val due = LocalDate.of(2026, 8, 1)

        viewModel.onEdit("Book eye exam", "Yearly", Priority.HIGH, due)

        assertEquals(TaskDetail("Book eye exam", "Yearly", Priority.HIGH, due), viewModel.uiState.value)
    }

    @Test
    fun `saving persists the edited task including its due date and emits Saved`() {
        val repository = FakeTaskRepository(listOf(task("Renew domain registration")))
        val viewModel = viewModelFor("Renew domain registration", repository)
        val events = mutableListOf<TaskDetailEvent>()
        val collector = scope.launch { viewModel.events.collect { events.add(it) } }
        val due = LocalDate.of(2026, 7, 25)

        viewModel.onEdit("Renew SSL certificate", "Expires next week", Priority.LOW, due)
        viewModel.onSave()

        assertEquals(listOf(TaskDetailEvent.Saved), events)
        val stored = runBlocking { repository.getTasks() }.single()
        assertEquals("Renew SSL certificate", stored.title)
        assertEquals("Expires next week", stored.notes)
        assertEquals(Priority.LOW, stored.priority)
        assertEquals(due, stored.dueDate)
        collector.cancel()
    }

    @Test
    fun `a failed save emits SaveFailed and leaves the task unchanged`() {
        val repository = FakeTaskRepository(listOf(task("Keep", notes = "original")))
        val viewModel = viewModelFor("Keep", repository)
        val events = mutableListOf<TaskDetailEvent>()
        val collector = scope.launch { viewModel.events.collect { events.add(it) } }

        repository.saveError = IOException("mock network save failed")
        viewModel.onEdit("Changed", "edited", Priority.HIGH, null)
        viewModel.onSave()

        assertEquals(listOf(TaskDetailEvent.SaveFailed), events)
        val stored = runBlocking { repository.getTasks() }.single()
        assertEquals("Keep", stored.title)
        assertEquals("original", stored.notes)
        collector.cancel()
    }
}
