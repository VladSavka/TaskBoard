package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject

/** Removes the task identified by [taskId]. */
class DeleteTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: String) = repository.deleteTask(taskId)
}
