package com.laschober.gymetrics.data.local

import com.russhwolf.settings.Settings

class SettingStore(
    private val settings: Settings = Settings(),
) {
    fun saveUrl(url: String) {
        settings.putString(URLKEY, url)
    }

    fun saveName(name: String) {
        settings.putString(NAMEKEY, name)
    }

    fun getName(): String? = settings.getStringOrNull(NAMEKEY)

    fun getUrl(): String? = settings.getStringOrNull(URLKEY)

    fun saveThemeMode(mode: ThemeMode) {
        settings.putString(THEMEMODEKEY, mode.name)
    }

    fun toggleSetup(mode: Boolean) {
        settings.putBoolean(SETUP, mode)
    }

    fun getSetupStatus() : Boolean? {
        return settings.getBooleanOrNull(SETUP)
    }

    fun setAiMode(mode: Boolean) {
        settings.putBoolean(AIMODE, mode)
    }

    fun getAiMode() : Boolean? {
        return settings.getBooleanOrNull(AIMODE)
    }

    fun getThemeMode(): ThemeMode =
        settings.getStringOrNull(THEMEMODEKEY)?.let { saved ->
            runCatching { ThemeMode.valueOf(saved) }.getOrNull()
        } ?: ThemeMode.DARK

    private companion object {
        const val URLKEY = "server_url"
        const val NAMEKEY = "name"
        const val THEMEMODEKEY = "theme_mode"
        const val SETUP = "setup"
        const val AIMODE = "ai_mode"
    }
}