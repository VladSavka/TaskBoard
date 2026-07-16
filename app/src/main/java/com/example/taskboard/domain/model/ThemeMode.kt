package com.example.taskboard.domain.model

/**
 * The user's theme preference. [label] is the human-facing name used in the UI
 * and in acceptance examples (e.g. "System").
 *
 * [SYSTEM] follows the device setting; [LIGHT] and [DARK] force a fixed theme
 * regardless of the device.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    /**
     * Whether this preference resolves to a dark appearance given whether the
     * device [systemInDarkMode]. [SYSTEM] defers to the device; the others are fixed.
     */
    fun isDark(systemInDarkMode: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkMode
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Resolves a [ThemeMode] from its [label], case-insensitively. */
        fun fromLabel(label: String): ThemeMode =
            entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown theme mode label: $label")
    }
}
