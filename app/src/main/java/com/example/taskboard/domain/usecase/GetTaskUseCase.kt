package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observes a single task by id (see [TaskRepository.task]); used to seed the detail screen. */
class GetTaskUseCase @Inject constructor(private val repository: TaskRepository) {
    operator fun invoke(id: String): Flow<Task?> = repository.task(id)
}
