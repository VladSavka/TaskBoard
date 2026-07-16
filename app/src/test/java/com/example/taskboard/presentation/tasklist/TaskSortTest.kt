package com.example.taskboard.presentation.tasklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskSortTest {

    @Test
    fun `each sort exposes its human-facing label`() {
        assertEquals("none", TaskSort.NONE.label)
        assertEquals("priority", TaskSort.PRIORITY.label)
        assertEquals("completion", TaskSort.COMPLETION.label)
    }

    @Test
    fun `fromLabel resolves each label case-insensitively`() {
        assertEquals(TaskSort.PRIORITY, TaskSort.fromLabel("priority"))
        assertEquals(TaskSort.COMPLETION, TaskSort.fromLabel("Completion"))
        assertEquals(TaskSort.NONE, TaskSort.fromLabel("  NONE  "))
    }

    @Test
    fun `fromLabel rejects an unknown label`() {
        assertThrows(IllegalArgumentException::class.java) { TaskSort.fromLabel("alphabetical") }
    }
}
