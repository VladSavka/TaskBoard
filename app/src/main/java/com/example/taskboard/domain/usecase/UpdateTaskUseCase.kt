package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject

/** Saves edits to an existing [task] (matched by [Task.id]). */
class UpdateTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.updateTask(task)
}
