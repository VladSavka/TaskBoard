package com.example.taskboard.presentation.tasklist

/**
 * One-shot side effects emitted by [TaskListViewModel], delivered via a
 * `SharedFlow` rather than living in [TaskListUiState] (per the MVVM rules for
 * transient events such as snackbars).
 */
sealed interface TaskBoardEvent {
    /** A save (add/toggle/delete/edit) failed; the change was not persisted. */
    data object SaveFailed : TaskBoardEvent

    /** A task was just deleted; the UI can offer an undo. */
    data object TaskDeleted : TaskBoardEvent
}
