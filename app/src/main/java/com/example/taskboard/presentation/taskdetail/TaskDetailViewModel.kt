package com.example.taskboard.presentation.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import com.example.taskboard.domain.usecase.GetTaskUseCase
import com.example.taskboard.domain.usecase.UpdateTaskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the task detail/edit screen. The task to edit is given by [taskId]
 * via the constructor (assisted injection); the ViewModel loads that task on
 * creation and owns its editable fields as observable [uiState]. Because a fresh
 * instance is created per opened task — scoped to the detail pane's lifecycle by the
 * host — there is nothing to reset when the detail closes. Saves go through
 * [updateTask] and their outcome is reported via one-shot [events]; the board
 * reloads on [TaskDetailEvent.Saved].
 *
 * All work runs on [viewModelScope]; tests and the acceptance harness supply a test
 * main dispatcher so those launches settle synchronously off the Android platform.
 */
@HiltViewModel(assistedFactory = TaskDetailViewModel.Factory::class)
class TaskDetailViewModel @AssistedInject constructor(
    @Assisted private val taskId: String,
    private val getTask: GetTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskDetail?>(null)
    /** The editable fields of the open task, or null while loading / if it no longer exists. */
    val uiState: StateFlow<TaskDetail?> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>(extraBufferCapacity = 8)
    /** One-shot outcomes of a save for the UI to react to. */
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    private var editing: Task? = null

    init {
        viewModelScope.launch {
            val task = getTask(taskId).first()
            editing = task
            _uiState.value = task?.let { TaskDetail(it.title, it.notes, it.priority, it.dueDate) }
        }
    }

    /** Records the edited fields into [uiState]. */
    fun onEdit(title: String, notes: String, priority: Priority, dueDate: LocalDate?) {
        _uiState.update { it?.copy(title = title, notes = notes, priority = priority, dueDate = dueDate) }
    }

    /** Persists the edits to the open task, emitting [TaskDetailEvent.Saved] on success. */
    fun onSave() {
        val target = editing ?: return
        val edits = _uiState.value ?: return
        viewModelScope.launch {
            try {
                updateTask(
                    target.copy(
                        title = edits.title,
                        notes = edits.notes,
                        priority = edits.priority,
                        dueDate = edits.dueDate,
                    ),
                )
                _events.tryEmit(TaskDetailEvent.Saved)
            } catch (t: Throwable) {
                _events.tryEmit(TaskDetailEvent.SaveFailed)
            }
        }
    }

    /** Creates a [TaskDetailViewModel] for a specific [taskId] (assisted injection). */
    @AssistedFactory
    interface Factory {
        fun create(taskId: String): TaskDetailViewModel
    }
}
