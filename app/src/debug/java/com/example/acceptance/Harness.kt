@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.acceptance

import androidx.lifecycle.SavedStateHandle
import com.example.taskboard.data.SampleTasks
import com.example.taskboard.data.datasource.InMemoryTaskDataSource
import com.example.taskboard.data.datasource.RemoteTaskDataSource
import com.example.taskboard.data.repository.DefaultTaskRepository
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.model.ThemeMode
import com.example.taskboard.domain.usecase.GetTaskUseCase
import com.example.taskboard.domain.usecase.UpdateTaskUseCase
import com.example.taskboard.presentation.taskdetail.TaskDetailEvent
import com.example.taskboard.presentation.taskdetail.TaskDetailViewModel
import com.example.taskboard.presentation.tasklist.DueLabel
import com.example.taskboard.presentation.tasklist.ListState
import com.example.taskboard.presentation.tasklist.TaskBoardEvent
import com.example.taskboard.presentation.tasklist.TaskListViewModel
import com.example.taskboard.presentation.tasklist.TaskSort
import com.example.taskboard.presentation.tasklist.TaskUseCases
import com.example.taskboard.presentation.tasklist.isAddActionAvailable
import com.example.taskboard.presentation.theme.ThemeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.setMain
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/**
 * Headless acceptance harness. Runs the Android-free product classes (domain,
 * data, ViewModel) on a plain JVM so the Babashka acceptance pipeline can drive
 * real behavior without an emulator.
 *
 * The [TaskListViewModel] uses the real `viewModelScope`; installing
 * [Dispatchers.Unconfined] as the main dispatcher makes its launches settle
 * synchronously within each call, so no injected scope is needed.
 *
 * Each command-line argument is one command whose fields are tab-separated. The
 * harness applies the commands in order against a real [TaskListViewModel] (and a
 * [ThemeViewModel] for theme steps); the final command is a query that prints a
 * single observable value to stdout.
 */

/** [RemoteTaskDataSource] whose read and write behavior is scripted per acceptance scenario. */
private class ScriptableTaskDataSource : RemoteTaskDataSource {
    var seeded: List<Task> = emptyList()
    var fault: String = "none"
    var failNextWrite: Boolean = false
    private var failedOnce = false

    override suspend fun readTasks(): List<Task> {
        when (fault) {
            "fail" -> throw IllegalStateException("scripted load failure")
            "fail_once" -> if (!failedOnce) {
                failedOnce = true
                throw IllegalStateException("scripted one-time load failure")
            }
            "pending" -> CompletableDeferred<Unit>().await() // never completes
        }
        return seeded
    }

    override suspend fun writeTasks(tasks: List<Task>) {
        if (failNextWrite) {
            failNextWrite = false
            throw IOException("scripted save failure")
        }
        seeded = tasks
    }
}

private class Board {
    private val dataSource = ScriptableTaskDataSource()
    private val repository = DefaultTaskRepository(dataSource, InMemoryTaskDataSource())
    private val useCases = TaskUseCases.from(repository) { UUID.randomUUID().toString() }

    /** Reference date for relative due-date labels; overridable per scenario. */
    private var today: LocalDate = LocalDate.now()

    /** A clock that always reflects the current [today], so [setToday] takes effect live. */
    private val clock: Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = today.atStartOfDay(ZoneOffset.UTC).toInstant()
    }

    // A fresh detail ViewModel per opened task (id via its constructor), mirroring the
    // app where the host builds one per selection and drops it on close — so there is
    // no shared instance to reset. Null means no detail is open.
    private var detailViewModel: TaskDetailViewModel? = null

    private val boardScope = CoroutineScope(Dispatchers.Unconfined)
    private var lastSaveFailed = false

    // Built lazily so scenario setup (seed/fault/today) is applied before the
    // ViewModel auto-loads on construction — mirroring the app, whose DI-seeded
    // data source is ready before Hilt builds the ViewModel. Realizing it (an
    // explicit load/launch, an action, or any state read) is what "the board
    // launches/finishes loading" means now that there is no separate load trigger.
    private var _viewModel: TaskListViewModel? = null

    /** The list ViewModel, built on first use; constructing it triggers its auto-load. */
    private val viewModel: TaskListViewModel
        get() = ensureViewModel()

    private fun ensureViewModel(): TaskListViewModel =
        _viewModel ?: newViewModel(initialQuery = "", initialSort = TaskSort.NONE)

    private fun newViewModel(initialQuery: String, initialSort: TaskSort): TaskListViewModel {
        val vm = TaskListViewModel(
            useCases = useCases,
            savedState = SavedStateHandle(
                mapOf(
                    TaskListViewModel.KEY_QUERY to initialQuery,
                    TaskListViewModel.KEY_SORT to initialSort.label,
                ),
            ),
            clock = clock,
        )
        boardScope.launch {
            vm.events.collect { if (it is TaskBoardEvent.SaveFailed) lastSaveFailed = true }
        }
        _viewModel = vm
        return vm
    }

    val uiState get() = viewModel.uiState.value
    val listState: ListState get() = uiState.list
    val tasks: List<Task> get() = (listState as? ListState.Content)?.items?.map { it.task } ?: emptyList()
    val query: String get() = uiState.query
    val sort: TaskSort get() = uiState.sort
    val addActionAvailable: Boolean get() = uiState.isAddActionAvailable
    // The empty-state message's stable resource key (an acceptance concern; production
    // resolves R.string.tasks_empty directly in the composable).
    val emptyMessageKey: String get() = if (listState is ListState.Empty) "tasks_empty" else ""
    val saveFailed: Boolean get() = lastSaveFailed
    val detailOpen: Boolean get() = detailViewModel?.uiState?.value != null

    /** Resolves a visible task's id from its (unique-in-acceptance) title. */
    private fun idOf(title: String): String? = tasks.firstOrNull { it.title == title }?.id

    fun detailShows(title: String, notes: String, priority: Priority): Boolean {
        val detail = detailViewModel?.uiState?.value ?: return false
        return detail.title == title && detail.notes == notes && detail.priority == priority
    }

    fun seed(task: Task) {
        dataSource.seeded = dataSource.seeded + task
    }

    fun seedDefault() {
        dataSource.seeded = SampleTasks.DEFAULT
    }

    fun count(n: Int) {
        dataSource.seeded = (1..n).map { Task(id = "seed-$it", title = "Task $it") }
    }

    fun fault(mode: String) {
        dataSource.fault = mode
    }

    fun failNextSave() {
        dataSource.failNextWrite = true
    }

    fun succeedNextSave() {
        dataSource.failNextWrite = false
    }

    fun setToday(date: LocalDate) {
        today = date
    }

    /** "Launching"/"finishing loading" just realizes the board, which loads itself on creation. */
    fun load() {
        ensureViewModel()
    }

    fun retry() = viewModel.onRetry()

    fun add(title: String, notes: String, priority: Priority) {
        lastSaveFailed = false
        viewModel.onAddTask(title, notes, priority, dueDate = null)
    }

    fun addWithDue(title: String, dueDate: LocalDate) {
        lastSaveFailed = false
        viewModel.onAddTask(title, "", Priority.MEDIUM, dueDate)
    }

    fun toggle(title: String) {
        idOf(title)?.let { viewModel.onToggleCompletion(it) }
    }

    fun delete(title: String) {
        idOf(title)?.let { viewModel.onDelete(it) }
    }

    fun undo() = viewModel.onUndoDelete()

    fun open(title: String) {
        val id = idOf(title) ?: return
        // Build a detail ViewModel for this task (loads on construction) and follow its
        // events, closing the detail on a saved edit as the host would on navigate-back.
        val vm = TaskDetailViewModel(id, GetTaskUseCase(repository), UpdateTaskUseCase(repository))
        detailViewModel = vm
        boardScope.launch {
            vm.events.collect {
                when (it) {
                    TaskDetailEvent.Saved -> detailViewModel = null
                    TaskDetailEvent.SaveFailed -> lastSaveFailed = true
                }
            }
        }
    }

    fun edit(title: String, notes: String, priority: Priority) {
        val vm = detailViewModel ?: return
        vm.onEdit(title, notes, priority, vm.uiState.value?.dueDate)
    }

    /** Sets just the open detail's due date, mirroring the date-picker wiring on the screen. */
    fun setDue(dueDate: LocalDate) {
        val vm = detailViewModel ?: return
        val detail = vm.uiState.value ?: return
        vm.onEdit(detail.title, detail.notes, detail.priority, dueDate)
    }

    fun save() {
        detailViewModel?.onSave()
    }

    fun search(query: String) = viewModel.onSearch(query)

    fun sort(sort: TaskSort) = viewModel.onSort(sort)

    /** Simulates a screen recreation: a new ViewModel that restores the saved query/sort and reloads on creation. */
    fun recreate() {
        val restoredQuery = uiState.query
        val restoredSort = uiState.sort
        _viewModel = null
        newViewModel(initialQuery = restoredQuery, initialSort = restoredSort)
    }

    fun dueLabel(title: String): String {
        val item = (listState as? ListState.Content)?.items?.firstOrNull { it.task.title == title }
        // The UI renders DueLabel via string resources; mirror that English text here,
        // since the headless harness has no Android resources to resolve.
        return when (val label = item?.dueLabel) {
            null -> ""
            DueLabel.Today -> "Today"
            DueLabel.Tomorrow -> "Tomorrow"
            DueLabel.Yesterday -> "Yesterday"
            is DueLabel.InDays -> "in ${label.days} days"
            is DueLabel.DaysAgo -> "${label.days} days ago"
        }
    }
}

/** Headless holder for the theme steps, mirroring the app's theme wiring. */
private class ThemeController {
    private val viewModel = ThemeViewModel()
    private var systemDark = false

    fun setSystemDark(value: Boolean) {
        systemDark = value
    }

    fun select(mode: ThemeMode) = viewModel.onSelectMode(mode)

    fun isDark(): Boolean = viewModel.selectedMode.value.isDark(systemDark)

    fun selectedLabel(): String = viewModel.selectedMode.value.label

    fun optionsLabel(): String = viewModel.options.joinToString(", ") { it.label }
}

private fun bool(raw: String): Boolean = raw.trim().equals("true", ignoreCase = true)

/** Reads a field that may be absent (e.g. an empty trailing search query). */
private fun List<String>.field(index: Int): String = getOrElse(index) { "" }

fun main(args: Array<String>) {
    // viewModelScope runs on Dispatchers.Main; Unconfined makes launches run
    // eagerly so state settles synchronously within each harness command.
    Dispatchers.setMain(Dispatchers.Unconfined)

    val board = Board()
    val theme = ThemeController()
    var output = ""

    for (arg in args) {
        val parts = arg.split('\t')
        when (parts[0]) {
            "seed" -> board.seed(
                Task(
                    id = UUID.randomUUID().toString(),
                    title = parts[1],
                    notes = parts[2],
                    priority = Priority.fromLabel(parts[3]),
                    done = bool(parts[4]),
                ),
            )
            "seed_due" -> board.seed(
                Task(id = UUID.randomUUID().toString(), title = parts[1], dueDate = LocalDate.parse(parts[2])),
            )
            "count" -> board.count(parts[1].toInt())
            "fault" -> board.fault(parts[1])
            "today" -> board.setToday(LocalDate.parse(parts[1]))
            "load" -> board.load()
            "retry" -> board.retry()
            "add" -> board.add(parts[1], parts[2], Priority.fromLabel(parts[3]))
            "add_due" -> board.addWithDue(parts[1], LocalDate.parse(parts[2]))
            "set_due" -> board.setDue(LocalDate.parse(parts[1]))
            "toggle" -> board.toggle(parts[1])
            "delete" -> board.delete(parts[1])
            "undo" -> board.undo()
            "open" -> board.open(parts[1])
            "edit" -> board.edit(parts[1], parts[2], Priority.fromLabel(parts[3]))
            "save" -> board.save()
            "search" -> board.search(parts.field(1))
            "sort" -> board.sort(TaskSort.fromLabel(parts[1]))
            "recreate" -> board.recreate()
            "seed_default" -> board.seedDefault()
            "net_fail_next_save" -> board.failNextSave()
            "net_succeed_next_save" -> board.succeedNextSave()
            "theme_system_dark" -> theme.setSystemDark(bool(parts[1]))
            "theme_select" -> theme.select(ThemeMode.fromLabel(parts[1]))

            "state" -> output = when (board.listState) {
                ListState.Loading -> "loading"
                ListState.Empty -> "empty"
                is ListState.Content -> "content"
                is ListState.Error -> "error"
            }
            "retry_available" -> output = ((board.listState as? ListState.Error)?.canRetry == true).toString()
            "visible_count" -> output = board.tasks.size.toString()
            "shown" -> output = board.tasks.any {
                it.title == parts[1] && it.priority == Priority.fromLabel(parts[2]) && it.done == bool(parts[3])
            }.toString()
            "shown_title" -> output = board.tasks.any { it.title == parts[1] }.toString()
            "title_at" -> output = board.tasks.getOrNull(parts[1].toInt() - 1)?.title ?: ""
            "active_query" -> output = board.query
            "active_sort" -> output = board.sort.label
            "saved_count" -> output = board.tasks.size.toString()
            "contains_title" -> output = board.tasks.any { it.title == parts[1] }.toString()
            "contains_full" -> output = board.tasks.any {
                it.title == parts[1] && it.notes == parts[2] &&
                    it.priority == Priority.fromLabel(parts[3]) && it.done == bool(parts[4])
            }.toString()
            "completion" -> output = (board.tasks.firstOrNull { it.title == parts[1] }?.done == true).toString()
            "due_label" -> output = board.dueLabel(parts[1])
            "add_action_available" -> output = board.addActionAvailable.toString()
            "empty_message_key" -> output = board.emptyMessageKey
            "save_result" -> output = if (board.saveFailed) "failed" else "succeeded"
            "detail_open" -> output = board.detailOpen.toString()
            "detail_shows" -> output =
                board.detailShows(parts[1], parts[2], Priority.fromLabel(parts[3])).toString()
            "app_in_dark_mode" -> output = theme.isDark().toString()
            "selected_theme" -> output = theme.selectedLabel()
            "theme_options" -> output = theme.optionsLabel()

            else -> throw IllegalArgumentException("unknown harness command: ${parts[0]}")
        }
    }

    println(output)
}
