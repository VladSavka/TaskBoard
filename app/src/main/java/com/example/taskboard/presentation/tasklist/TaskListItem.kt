package com.example.taskboard.presentation.tasklist

import com.example.taskboard.domain.model.Task

/** A visible task row: the [task] plus its relative due-date [dueLabel] descriptor (null when none). */
data class TaskListItem(val task: Task, val dueLabel: DueLabel?)
