package com.example.taskboard.data

import com.example.taskboard.domain.model.Priority
import com.example.taskboard.domain.model.Task
import java.time.LocalDate

/**
 * The built-in sample seed the board opens with, so the app shows real content
 * instead of an empty screen on launch. This single constant feeds both the
 * app's data source (via the DI graph) and the Sample Seed acceptance feature.
 *
 * A few tasks carry due dates (relative to today) so the list shows off its
 * due-date labels; one is left without so the "no due date" case is visible too.
 */
object SampleTasks {
    val DEFAULT: List<Task>
        get() = listOf(
            Task(
                id = "sample-1",
                title = "Renew domain registration",
                priority = Priority.HIGH,
                done = false,
                dueDate = LocalDate.now().plusDays(3),
            ),
            Task(
                id = "sample-2",
                title = "Reply to design feedback",
                priority = Priority.MEDIUM,
                done = false,
                dueDate = LocalDate.now().plusDays(1),
            ),
            Task(id = "sample-3", title = "Book dentist", priority = Priority.LOW, done = true),
            Task(
                id = "sample-4",
                title = "Migrate the analytics pipeline to the new warehouse and validate dashboards",
                priority = Priority.MEDIUM,
                done = false,
                dueDate = LocalDate.now().plusWeeks(1),
            ),
        )
}
