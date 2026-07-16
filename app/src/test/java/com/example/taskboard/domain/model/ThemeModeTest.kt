package com.example.taskboard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system follows the device dark-mode setting`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkMode = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkMode = false))
    }

    @Test
    fun `light is never dark regardless of the device`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkMode = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkMode = false))
    }

    @Test
    fun `dark is always dark regardless of the device`() {
        assertTrue(ThemeMode.DARK.isDark(systemInDarkMode = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkMode = false))
    }

    @Test
    fun `fromLabel resolves each label case-insensitively`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromLabel("System"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromLabel("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromLabel("  DARK  "))
    }

    @Test
    fun `fromLabel rejects an unknown label`() {
        assertThrows(IllegalArgumentException::class.java) { ThemeMode.fromLabel("Sepia") }
    }
}
