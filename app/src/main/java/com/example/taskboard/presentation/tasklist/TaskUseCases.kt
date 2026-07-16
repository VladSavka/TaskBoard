package com.example.taskboard.presentation.tasklist

import com.example.taskboard.domain.repository.TaskRepository
import com.example.taskboard.domain.usecase.AddTaskUseCase
import com.example.taskboard.domain.usecase.DeleteTaskUseCase
import com.example.taskboard.domain.usecase.LoadTasksUseCase
import com.example.taskboard.domain.usecase.RestoreTaskUseCase
import com.example.taskboard.domain.usecase.ToggleTaskCompletionUseCase
import com.example.taskboard.domain.usecase.UpdateTaskUseCase
import javax.inject.Inject

/**
 * The task board's use cases, grouped so [TaskListViewModel] takes one dependency
 * instead of six. In production Hilt assembles it by constructor injection (each use
 * case is `@Inject`-constructable); [from] is a convenience for building the set from
 * a repository outside DI — the unit tests and the headless acceptance harness.
 */
data class TaskUseCases @Inject constructor(
    val load: LoadTasksUseCase,
    val add: AddTaskUseCase,
    val toggleCompletion: ToggleTaskCompletionUseCase,
    val delete: DeleteTaskUseCase,
    val restore: RestoreTaskUseCase,
    val update: UpdateTaskUseCase,
) {
    companion object {
        /** Builds the full set from a [repository], using [generateId] for new task identities. */
        fun from(repository: TaskRepository, generateId: () -> String) = TaskUseCases(
            load = LoadTasksUseCase(repository),
            add = AddTaskUseCase(repository) { generateId() },
            toggleCompletion = ToggleTaskCompletionUseCase(repository),
            delete = DeleteTaskUseCase(repository),
            restore = RestoreTaskUseCase(repository),
            update = UpdateTaskUseCase(repository),
        )
    }
}
