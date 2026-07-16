package com.example.taskboard.data

import com.example.taskboard.domain.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleTasksTest {

    @Test
    fun `the default seed holds the four sample tasks`() {
        assertEquals(4, SampleTasks.DEFAULT.size)
        assertEquals(
            listOf(
                "Renew domain registration",
                "Reply to design feedback",
                "Book dentist",
                "Migrate the analytics pipeline to the new warehouse and validate dashboards",
            ),
            SampleTasks.DEFAULT.map { it.title },
        )
    }

    @Test
    fun `each sample task carries its expected priority and completion`() {
        val byTitle = SampleTasks.DEFAULT.associateBy { it.title }

        assertEquals(Priority.HIGH, byTitle.getValue("Renew domain registration").priority)
        assertEquals(Priority.MEDIUM, byTitle.getValue("Reply to design feedback").priority)
        assertEquals(Priority.LOW, byTitle.getValue("Book dentist").priority)

        assertEquals(true, byTitle.getValue("Book dentist").done)
        assertEquals(false, byTitle.getValue("Renew domain registration").done)
    }

    @Test
    fun `sample task ids are unique`() {
        val ids = SampleTasks.DEFAULT.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
