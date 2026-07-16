package com.example.taskboard.presentation.tasklist

/**
 * How the visible task list is ordered. [label] is the human-facing name used in
 * the UI and in acceptance examples (e.g. "priority").
 *
 * [NONE] keeps insertion order; [PRIORITY] orders High → Medium → Low; [COMPLETION]
 * puts incomplete tasks before completed ones. Ordering is otherwise stable.
 */
enum class TaskSort(val label: String) {
    NONE("none"),
    PRIORITY("priority"),
    COMPLETION("completion");

    companion object {
        /** Resolves a [TaskSort] from its [label], case-insensitively. */
        fun fromLabel(label: String): TaskSort =
            entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown sort label: $label")
    }
}
