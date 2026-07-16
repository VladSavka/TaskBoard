package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject

/** Puts a previously deleted [task] back on the board, preserving its identity. */
class RestoreTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.addTask(task)
}
