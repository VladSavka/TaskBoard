package com.example.taskboard.domain.usecase

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Adds a new, incomplete task. A blank title is not a valid task: nothing is added
 * and the board is left as-is.
 */
class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(title: String, notes: String, priority: Priority, dueDate: LocalDate?) {
        if (title.isBlank()) return
        repository.addTask(
            Task(
                id = idGenerator.next(),
                title = title,
                notes = notes,
                priority = priority,
                dueDate = dueDate,
                done = false,
            ),
        )
    }
}
