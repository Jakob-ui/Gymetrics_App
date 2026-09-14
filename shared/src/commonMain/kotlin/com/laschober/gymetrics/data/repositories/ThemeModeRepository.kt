package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeModeRepository(
    private val settingStore: SettingStore,
) {
    private val _themeMode = MutableStateFlow(settingStore.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settingStore.saveThemeMode(mode)
        _themeMode.value = mode
    }
}
