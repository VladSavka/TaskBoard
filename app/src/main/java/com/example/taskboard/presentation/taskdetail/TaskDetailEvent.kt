package com.example.taskboard.presentation.taskdetail

/**
 * One-shot side effects emitted by [TaskDetailViewModel], delivered via a
 * `SharedFlow`. The board reacts to [Saved] (reload + return to the list); the
 * detail screen surfaces [SaveFailed] (e.g. a snackbar) and stays open.
 */
sealed interface TaskDetailEvent {
    /** The edit was persisted; the caller should return to the list. */
    data object Saved : TaskDetailEvent

    /** The save failed; the edit was not persisted and the detail stays open. */
    data object SaveFailed : TaskDetailEvent
}
