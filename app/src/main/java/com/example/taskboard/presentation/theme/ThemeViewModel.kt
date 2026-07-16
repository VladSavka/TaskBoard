package com.example.taskboard.presentation.theme

import androidx.lifecycle.ViewModel
import com.example.taskboard.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Theme preference ViewModel. Owns the currently [selectedMode] (defaulting to
 * [ThemeMode.SYSTEM]). The effective dark/light appearance is derived at the UI
 * layer from [selectedMode] plus the device's dark-mode setting (via
 * [ThemeMode.isDark], read at composition time) — the ViewModel exposes only state.
 *
 * It uses no coroutines and no Android types beyond [ViewModel], so it is fully
 * testable off the platform and drivable headlessly by the acceptance harness.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    private val _selectedMode = MutableStateFlow(ThemeMode.SYSTEM)
    val selectedMode: StateFlow<ThemeMode> = _selectedMode.asStateFlow()

    /** The theme options offered in the selection dialog, in display order. */
    val options: List<ThemeMode> = ThemeMode.entries

    /** Selects a new theme preference. */
    fun onSelectMode(mode: ThemeMode) {
        _selectedMode.value = mode
    }
}
