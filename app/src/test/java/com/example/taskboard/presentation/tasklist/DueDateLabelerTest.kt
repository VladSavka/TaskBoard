package com.example.taskboard.presentation.tasklist

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DueDateLabelerTest {

    private val labeler = DueDateLabeler()
    private val today = LocalDate.of(2026, 7, 15)

    private fun labelFor(due: String): DueLabel =
        labeler.label(LocalDate.parse(due), today)

    @Test
    fun `a task due today is labelled Today`() {
        assertEquals(DueLabel.Today, labelFor("2026-07-15"))
    }

    @Test
    fun `a task due tomorrow is labelled Tomorrow`() {
        assertEquals(DueLabel.Tomorrow, labelFor("2026-07-16"))
    }

    @Test
    fun `a task due yesterday is labelled Yesterday`() {
        assertEquals(DueLabel.Yesterday, labelFor("2026-07-14"))
    }

    @Test
    fun `a task due several days ahead counts the days forward`() {
        assertEquals(DueLabel.InDays(3), labelFor("2026-07-18"))
    }

    @Test
    fun `a task overdue by several days counts the days back`() {
        assertEquals(DueLabel.DaysAgo(5), labelFor("2026-07-10"))
    }

    @Test
    fun `the relative label spans month boundaries`() {
        // 5 days from June 30 lands on July 5, crossing the month boundary.
        assertEquals(DueLabel.InDays(5), labeler.label(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 6, 30)))
        // 3 days before July 2 lands on June 29.
        assertEquals(DueLabel.DaysAgo(3), labeler.label(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2)))
    }
}
