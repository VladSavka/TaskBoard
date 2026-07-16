package com.example.taskboard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PriorityTest {

    @Test
    fun `maps each label to its priority`() {
        assertEquals(Priority.HIGH, Priority.fromLabel("High"))
        assertEquals(Priority.MEDIUM, Priority.fromLabel("Medium"))
        assertEquals(Priority.LOW, Priority.fromLabel("Low"))
    }

    @Test
    fun `resolves labels case-insensitively and ignores surrounding space`() {
        assertEquals(Priority.HIGH, Priority.fromLabel("high"))
        assertEquals(Priority.LOW, Priority.fromLabel("  LOW  "))
    }

    @Test
    fun `rejects an unknown label`() {
        assertThrows(IllegalArgumentException::class.java) {
            Priority.fromLabel("Critical")
        }
    }
}
