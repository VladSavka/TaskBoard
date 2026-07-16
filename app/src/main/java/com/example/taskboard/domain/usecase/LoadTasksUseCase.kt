package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observes the task collection, re-emitting after every mutation (see [TaskRepository.loadTasks]). */
class LoadTasksUseCase @Inject constructor(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repository.loadTasks()
}
