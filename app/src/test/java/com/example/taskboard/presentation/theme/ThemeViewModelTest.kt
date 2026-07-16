package com.example.taskboard.presentation.theme

import com.example.taskboard.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeViewModelTest {

    @Test
    fun `the default selected theme is System`() {
        val viewModel = ThemeViewModel()

        assertEquals(ThemeMode.SYSTEM, viewModel.selectedMode.value)
    }

    @Test
    fun `the theme options are System, Light, Dark in order`() {
        val viewModel = ThemeViewModel()

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK), viewModel.options)
        assertEquals("System, Light, Dark", viewModel.options.joinToString(", ") { it.label })
    }

    @Test
    fun `selecting a theme updates the selected mode`() {
        val viewModel = ThemeViewModel()

        viewModel.onSelectMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.selectedMode.value)
    }

    @Test
    fun `forcing dark ignores the device setting`() {
        val viewModel = ThemeViewModel()

        viewModel.onSelectMode(ThemeMode.DARK)

        assertTrue(viewModel.selectedMode.value.isDark(systemInDarkMode = false))
    }

    @Test
    fun `forcing light ignores the device setting`() {
        val viewModel = ThemeViewModel()

        viewModel.onSelectMode(ThemeMode.LIGHT)

        assertFalse(viewModel.selectedMode.value.isDark(systemInDarkMode = true))
    }

    @Test
    fun `system mode follows the device setting`() {
        val viewModel = ThemeViewModel()

        viewModel.onSelectMode(ThemeMode.SYSTEM)

        assertTrue(viewModel.selectedMode.value.isDark(systemInDarkMode = true))
        assertFalse(viewModel.selectedMode.value.isDark(systemInDarkMode = false))
    }
}
