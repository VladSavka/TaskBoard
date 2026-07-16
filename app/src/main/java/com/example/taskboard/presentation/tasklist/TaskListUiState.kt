package com.example.taskboard.presentation.tasklist

/**
 * Immutable UI state for the task list screen. Everything the screen renders lives
 * here: the active search [query] and [sort] (also mirrored into the ViewModel's
 * SavedStateHandle so they survive recreation) and the derived [list]. Opening a
 * task for editing is navigation handled by the board host, not state here.
 */
data class TaskListUiState(
    val query: String = "",
    val sort: TaskSort = TaskSort.NONE,
    val list: ListState = ListState.Loading,
)

/** The load/derivation status of the visible list. */
sealed interface ListState {
    /** A load is in flight. */
    data object Loading : ListState

    /** Loading finished and there are no tasks. */
    data object Empty : ListState

    /** Loading finished with at least one task; [items] are the visible rows. */
    data class Content(val items: List<TaskListItem>) : ListState

    /** Loading failed. [canRetry] reports whether a retry action is offered. */
    data class Error(val canRetry: Boolean = true) : ListState
}

/**
 * Whether the add-task affordance is offered. It is available from every settled
 * state (empty, content, error) so a task can always be added, and hidden only
 * while the initial load is still in flight.
 */
val TaskListUiState.isAddActionAvailable: Boolean
    get() = list !is ListState.Loading
