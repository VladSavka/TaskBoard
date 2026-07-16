package com.example.taskboard.presentation.tasklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Task list ViewModel. Collects the repository's task [Flow] and projects each
 * emission — combined with the current search query and sort — into [uiState].
 * Because the repository re-emits after every mutation (optimistically), the list
 * updates immediately and rolls back on a failed save; there is no locally cached
 * task list here, only the derived view state.
 *
 * Retry cancels the in-flight collection ([observeJob]) and re-collects. The query
 * and sort live in [uiState] (mirrored into [savedState] so they survive recreation).
 * Opening a task for editing is navigation, handled by the board host, not state
 * here. The [clock] is injected so the harness and tests can pin the reference date
 * for due-date labels.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val useCases: TaskUseCases,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {

    private val dueLabeler = DueDateLabeler()

    private val _uiState = MutableStateFlow(
        TaskListUiState(
            query = savedState[KEY_QUERY] ?: "",
            sort = TaskSort.fromLabel(savedState[KEY_SORT] ?: TaskSort.NONE.label),
        ),
    )
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskBoardEvent>(extraBufferCapacity = 8)
    /** One-shot side effects (e.g. a failed save) for the UI to react to. */
    val events: SharedFlow<TaskBoardEvent> = _events.asSharedFlow()

    /**
     * Handles a failed mutation: the repository reverts the optimistic change (the
     * list flow re-emits the rollback), and we surface a one-shot [TaskBoardEvent.SaveFailed].
     * Used as the context for each mutation launch, so no per-call try/catch is needed.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        _events.tryEmit(TaskBoardEvent.SaveFailed)
    }

    private var lastDeleted: Task? = null
    private var observeJob: Job? = null

    init {
        observe()
    }

    /**
     * Collects the repository task flow — loading first, then re-emitting on every
     * mutation, or an error — combined with the current query and sort, projecting
     * each emission straight into [ListState]. The preferences are read back off
     * [uiState] so a search/sort change re-derives the list. Retry cancels this
     * collection and re-collects.
     */
    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val prefs = _uiState.map { ViewPrefs(it.query, it.sort) }.distinctUntilChanged()
            combine(useCases.load(), prefs) { tasks, p -> toListState(tasks, p) }
                .onStart { emit(ListState.Loading) }
                .catch { emit(ListState.Error()) }
                .collect { list -> _uiState.update { it.copy(list = list) } }
        }
    }

    /** Retries a failed load. */
    fun onRetry() = observe()

    /** Sets the active search query, persists it, and re-derives the visible list. */
    fun onSearch(query: String) {
        savedState[KEY_QUERY] = query
        _uiState.update { it.copy(query = query) }
    }

    /** Sets the active sort, persists it, and re-derives the visible list. */
    fun onSort(sort: TaskSort) {
        savedState[KEY_SORT] = sort.label
        _uiState.update { it.copy(sort = sort) }
    }

    fun onAddTask(title: String, notes: String, priority: Priority, dueDate: LocalDate?) {
        viewModelScope.launch(exceptionHandler) { useCases.add.invoke(title, notes, priority, dueDate) }
    }

    fun onToggleCompletion(id: String) {
        val task = findById(id) ?: return
        viewModelScope.launch(exceptionHandler) { useCases.toggleCompletion(task) }
    }

    fun onDelete(id: String) {
        val task = findById(id) ?: return
        lastDeleted = task
        viewModelScope.launch(exceptionHandler) {
            useCases.delete(id)
            _events.tryEmit(TaskBoardEvent.TaskDeleted)
        }
    }

    fun onUndoDelete() {
        val task = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch(exceptionHandler) { useCases.restore(task) }
    }

    /** Maps the loaded [tasks] to the visible rows, attaching each row's due-date label. */
    private fun toListState(tasks: List<Task>, p: ViewPrefs): ListState =
        if (tasks.isEmpty()) {
            ListState.Empty
        } else {
            ListState.Content(filteredSorted(tasks, p.query, p.sort).map { TaskListItem(it, dueLabel(it)) })
        }

    /** The visible subset of [tasks]: a case-insensitive title [query] filter, then the [sort] ordering. */
    private fun filteredSorted(tasks: List<Task>, query: String, sort: TaskSort): List<Task> {
        val filtered =
            if (query.isBlank()) tasks
            else tasks.filter { it.title.contains(query.trim(), ignoreCase = true) }
        return when (sort) {
            TaskSort.NONE -> filtered
            TaskSort.PRIORITY -> filtered.sortedBy { it.priority.ordinal }
            TaskSort.COMPLETION -> filtered.sortedBy { it.done }
        }
    }

    private fun dueLabel(task: Task): DueLabel? =
        task.dueDate?.let { dueLabeler.label(it, LocalDate.now(clock)) }

    /** Resolves a visible task by its stable id (titles are not unique). */
    private fun findById(id: String): Task? =
        (_uiState.value.list as? ListState.Content)?.items?.firstOrNull { it.task.id == id }?.task

    private data class ViewPrefs(val query: String, val sort: TaskSort)

    companion object {
        internal const val KEY_QUERY = "taskboard.query"
        internal const val KEY_SORT = "taskboard.sort"
    }
}
