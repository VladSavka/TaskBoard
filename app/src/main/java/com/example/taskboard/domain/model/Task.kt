package com.example.taskboard.domain.model

import java.time.LocalDate

/**
 * A single task on the board. [id] is the stable identity that survives edits;
 * user-visible fields ([title], [notes], [priority], [done], [dueDate]) may all
 * change. [dueDate] is optional — a null value means the task has no due date.
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val dueDate: LocalDate? = null,
)
