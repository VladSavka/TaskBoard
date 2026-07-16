package com.example.taskboard.presentation.tasklist

import androidx.lifecycle.SavedStateHandle
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.testsupport.FakeTaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun installMainDispatcher() {
        // viewModelScope runs on Dispatchers.Main; Unconfined makes launches run
        // eagerly so state settles synchronously within each call under test.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun savedStateOf(query: String = "", sort: TaskSort = TaskSort.NONE) =
        SavedStateHandle(
            mapOf(
                TaskListViewModel.KEY_QUERY to query,
                TaskListViewModel.KEY_SORT to sort.label,
            ),
        )

    private fun viewModelWith(
        repository: FakeTaskRepository,
        savedState: SavedStateHandle = SavedStateHandle(),
        today: LocalDate = LocalDate.of(2026, 7, 15),
    ): TaskListViewModel {
        var counter = 0
        return TaskListViewModel(
            useCases = TaskUseCases.from(repository) { "id-${counter++}" },
            savedState = savedState,
            clock = fixedClockAt(today),
        )
    }

    /** A clock pinned so that `LocalDate.now(clock)` is [date]. */
    private fun fixedClockAt(date: LocalDate): Clock =
        Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun task(
        title: String,
        done: Boolean = false,
        priority: Priority = Priority.MEDIUM,
        dueDate: LocalDate? = null,
    ) = Task(id = title, title = title, notes = "", priority = priority, done = done, dueDate = dueDate)

    /** All view reads go through [TaskListViewModel.uiState]; there are no value-returning getters. */
    private fun visibleList(viewModel: TaskListViewModel): ListState = viewModel.uiState.value.list

    private fun items(viewModel: TaskListViewModel): List<TaskListItem> =
        (visibleList(viewModel) as ListState.Content).items

    private fun visibleTitles(viewModel: TaskListViewModel): List<String> =
        items(viewModel).map { it.task.title }

    private fun itemFor(viewModel: TaskListViewModel, title: String): TaskListItem? =
        (visibleList(viewModel) as? ListState.Content)?.items?.firstOrNull { it.task.title == title }

    @Test
    fun `the board loads itself on creation, with no explicit trigger`() {
        val viewModel = viewModelWith(FakeTaskRepository(listOf(task("A"), task("B"))))

        // No load() call: constructing the ViewModel is enough to drive it to content.
        assertEquals(listOf("A", "B"), visibleTitles(viewModel))
    }

    @Test
    fun `finishing a load with no tasks shows Empty`() {
        val viewModel = viewModelWith(FakeTaskRepository())


        assertEquals(ListState.Empty, visibleList(viewModel))
    }

    @Test
    fun `finishing a load with tasks shows Content`() {
        val viewModel = viewModelWith(FakeTaskRepository(listOf(task("A"), task("B"))))


        assertEquals(listOf("A", "B"), visibleTitles(viewModel))
    }

    @Test
    fun `an in-flight read shows Loading until it completes`() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeTaskRepository().apply { loadGate = gate }
        val viewModel = viewModelWith(repository)

        // The board's auto-load on creation stays in flight until the read completes.
        assertEquals(ListState.Loading, visibleList(viewModel))

        gate.complete(Unit)
        assertEquals(ListState.Empty, visibleList(viewModel))
    }

    @Test
    fun `a failed load shows Error with a retry action`() {
        val repository = FakeTaskRepository().apply { loadError = IOException("boom") }
        val viewModel = viewModelWith(repository)


        val error = visibleList(viewModel) as ListState.Error
        assertTrue(error.canRetry)
    }

    @Test
    fun `retry recovers after a one-time failure`() {
        val repository = FakeTaskRepository(listOf(task("A"), task("B"), task("C")))
            .apply { failLoadOnce = true }
        val viewModel = viewModelWith(repository)

        // The auto-load on creation fails once, leaving the board in Error.
        assertTrue(visibleList(viewModel) is ListState.Error)

        viewModel.onRetry()
        assertEquals(3, items(viewModel).size)
    }

    @Test
    fun `adding a task shows it as incomplete content`() {
        val viewModel = viewModelWith(FakeTaskRepository())

        viewModel.onAddTask("Renew domain registration", "Expires end of month", Priority.HIGH, dueDate = null)

        val added = items(viewModel).single().task
        assertEquals("Renew domain registration", added.title)
        assertFalse(added.done)
    }

    @Test
    fun `adding a blank-titled task leaves the board empty`() {
        val viewModel = viewModelWith(FakeTaskRepository())

        viewModel.onAddTask("", "notes without a title", Priority.MEDIUM, dueDate = null)

        assertEquals(ListState.Empty, visibleList(viewModel))
    }

    @Test
    fun `toggling completion by title flips the flag`() {
        val viewModel = viewModelWith(FakeTaskRepository(listOf(task("Book dentist", done = false))))

        viewModel.onToggleCompletion("Book dentist")

        assertTrue(items(viewModel).single().task.done)
    }

    @Test
    fun `undoing a delete restores the task`() {
        val viewModel = viewModelWith(FakeTaskRepository(listOf(task("Keep"), task("Drop"))))

        viewModel.onDelete("Drop")
        assertFalse(visibleTitles(viewModel).contains("Drop"))

        viewModel.onUndoDelete()
        assertTrue(visibleTitles(viewModel).contains("Drop"))
        assertTrue(visibleTitles(viewModel).contains("Keep"))
    }

    @Test
    fun `searching keeps only tasks whose title contains the query, ignoring case`() {
        val repository = FakeTaskRepository(
            listOf(task("Renew domain registration"), task("Reply to design feedback"), task("Book dentist")),
        )
        val viewModel = viewModelWith(repository)

        viewModel.onSearch("DESIGN")

        assertEquals(listOf("Reply to design feedback"), visibleTitles(viewModel))
        assertEquals("DESIGN", viewModel.uiState.value.query)
    }

    @Test
    fun `a blank search shows every task`() {
        val repository = FakeTaskRepository(listOf(task("Renew domain registration"), task("Book dentist")))
        val viewModel = viewModelWith(repository)

        viewModel.onSearch("dentist")
        viewModel.onSearch("")

        assertEquals(listOf("Renew domain registration", "Book dentist"), visibleTitles(viewModel))
    }

    @Test
    fun `sorting by priority orders high to low`() {
        val repository = FakeTaskRepository(
            listOf(
                task("Book dentist", priority = Priority.LOW),
                task("Renew domain registration", priority = Priority.HIGH),
                task("Reply to design feedback", priority = Priority.MEDIUM),
            ),
        )
        val viewModel = viewModelWith(repository)

        viewModel.onSort(TaskSort.PRIORITY)

        assertEquals(
            listOf("Renew domain registration", "Reply to design feedback", "Book dentist"),
            visibleTitles(viewModel),
        )
    }

    @Test
    fun `sorting by completion puts incomplete tasks first and is otherwise stable`() {
        val repository = FakeTaskRepository(
            listOf(
                task("Book dentist", done = true),
                task("Renew domain registration", done = false),
                task("Reply to design feedback", done = true),
            ),
        )
        val viewModel = viewModelWith(repository)

        viewModel.onSort(TaskSort.COMPLETION)

        assertEquals(
            listOf("Renew domain registration", "Book dentist", "Reply to design feedback"),
            visibleTitles(viewModel),
        )
    }

    @Test
    fun `a recreated ViewModel restores the saved query and sort into the view state`() {
        val repository = FakeTaskRepository(
            listOf(
                task("Renew domain registration", priority = Priority.HIGH),
                task("Reply to design feedback", priority = Priority.MEDIUM),
            ),
        )
        val recreated = viewModelWith(
            repository,
            savedState = savedStateOf(query = "re", sort = TaskSort.PRIORITY),
        )

        assertEquals("re", recreated.uiState.value.query)
        assertEquals(TaskSort.PRIORITY, recreated.uiState.value.sort)
        assertEquals(
            listOf("Renew domain registration", "Reply to design feedback"),
            visibleTitles(recreated),
        )
    }

    @Test
    fun `search and sort survive into the SavedStateHandle`() {
        val savedState = savedStateOf()
        val viewModel = viewModelWith(FakeTaskRepository(listOf(task("A"))), savedState = savedState)

        viewModel.onSearch("a")
        viewModel.onSort(TaskSort.PRIORITY)

        assertEquals("a", savedState[TaskListViewModel.KEY_QUERY])
        assertEquals(TaskSort.PRIORITY.label, savedState[TaskListViewModel.KEY_SORT])
    }

    @Test
    fun `the due label rides in the view item, relative to today`() {
        val repository = FakeTaskRepository(
            listOf(task("File taxes", dueDate = LocalDate.of(2026, 7, 18))),
        )
        val viewModel = viewModelWith(repository, today = LocalDate.of(2026, 7, 15))

        assertEquals(DueLabel.InDays(3), itemFor(viewModel, "File taxes")?.dueLabel)
    }

    @Test
    fun `a task with no due date has no due label`() {
        val repository = FakeTaskRepository(listOf(task("Book dentist")))
        val viewModel = viewModelWith(repository)

        assertNull(itemFor(viewModel, "Book dentist")?.dueLabel)
    }

    @Test
    fun `a failed save emits SaveFailed and leaves the board unchanged`() {
        val repository = FakeTaskRepository(listOf(task("Keep")))
        val viewModel = viewModelWith(repository)
        val events = mutableListOf<TaskBoardEvent>()
        val collector = scope.launch { viewModel.events.collect { events.add(it) } }

        repository.saveError = IOException("mock network save failed")
        viewModel.onAddTask("Book gym", "", Priority.LOW, dueDate = null)

        assertEquals(listOf(TaskBoardEvent.SaveFailed), events)
        assertEquals(listOf("Keep"), visibleTitles(viewModel))
        collector.cancel()
    }

    @Test
    fun `a successful save emits no failure event`() {
        val viewModel = viewModelWith(FakeTaskRepository())
        val events = mutableListOf<TaskBoardEvent>()
        val collector = scope.launch { viewModel.events.collect { events.add(it) } }

        viewModel.onAddTask("Book gym", "", Priority.LOW, dueDate = null)

        assertTrue(events.isEmpty())
        assertEquals(listOf("Book gym"), visibleTitles(viewModel))
        collector.cancel()
    }
}
