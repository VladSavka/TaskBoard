package com.example.taskboard.presentation.tasklist

import com.example.taskboard.domain.model.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskListUiStateTest {

    private fun stateWith(list: ListState) = TaskListUiState(list = list)

    private val anItem = TaskListItem(Task(id = "1", title = "A"), dueLabel = null)

    @Test
    fun `the add-task action is available from every settled state`() {
        assertTrue(stateWith(ListState.Empty).isAddActionAvailable)
        assertTrue(stateWith(ListState.Content(listOf(anItem))).isAddActionAvailable)
        assertTrue(stateWith(ListState.Error()).isAddActionAvailable)
    }

    @Test
    fun `the add-task action is hidden while loading`() {
        assertFalse(stateWith(ListState.Loading).isAddActionAvailable)
    }
}
