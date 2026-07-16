package com.example.taskboard.domain.model

/**
 * Importance of a task. [label] is the human-facing name used in the UI and in
 * acceptance examples (e.g. "High").
 */
enum class Priority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        /** Resolves a [Priority] from its [label], case-insensitively. */
        fun fromLabel(label: String): Priority =
            entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown priority label: $label")
    }
}
