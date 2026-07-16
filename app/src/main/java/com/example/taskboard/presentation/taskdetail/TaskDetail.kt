package com.example.taskboard.presentation.taskdetail

import com.example.taskboard.domain.model.Priority
import java.time.LocalDate

/**
 * The editable fields of the task open on the detail/edit screen. Exposed as the
 * observable state of [TaskDetailViewModel] so the detail screen renders from the
 * ViewModel rather than from the tapped row. [dueDate] is null when the task has none.
 */
data class TaskDetail(
    val title: String,
    val notes: String,
    val priority: Priority,
    val dueDate: LocalDate?,
)
