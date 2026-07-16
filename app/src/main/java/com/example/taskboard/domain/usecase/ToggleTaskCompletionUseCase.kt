package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject

/** Flips the completion flag of [task]. The caller supplies the task to toggle. */
class ToggleTaskCompletionUseCase @Inject constructor(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) =
        repository.updateTask(task.copy(done = !task.done))
}
