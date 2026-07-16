package com.example.taskboard.presentation.tasklist

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Classifies a task's due date into a relative [DueLabel] — Today, Tomorrow,
 * Yesterday, InDays, or DaysAgo — measured against a reference [today]. Pure logic
 * with no Android dependencies; the UI renders the label to text via resources.
 */
class DueDateLabeler {
    /** The relative label for a task [due] on the given date, relative to [today]. */
    fun label(due: LocalDate, today: LocalDate): DueLabel {
        val days = ChronoUnit.DAYS.between(today, due).toInt()
        return when {
            days == 0 -> DueLabel.Today
            days == 1 -> DueLabel.Tomorrow
            days == -1 -> DueLabel.Yesterday
            days > 0 -> DueLabel.InDays(days)
            else -> DueLabel.DaysAgo(-days)
        }
    }
}

/**
 * A task's relative due date as a display-agnostic descriptor. [DueDateLabeler]
 * decides which case applies (in the ViewModel, with no Android dependency); the UI
 * layer renders it to text via string resources, so the wording stays translatable.
 */
sealed interface DueLabel {
    data object Today : DueLabel
    data object Tomorrow : DueLabel
    data object Yesterday : DueLabel

    /** Due [days] in the future (always ≥ 2; one day ahead is [Tomorrow]). */
    data class InDays(val days: Int) : DueLabel

    /** Overdue by [days] (always ≥ 2; one day behind is [Yesterday]). */
    data class DaysAgo(val days: Int) : DueLabel
}
