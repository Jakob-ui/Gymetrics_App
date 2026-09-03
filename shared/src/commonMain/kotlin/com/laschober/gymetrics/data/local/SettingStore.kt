package com.laschober.gymetrics.data.local

import com.russhwolf.settings.Settings

class SettingStore(
    private val settings: Settings = Settings(),
) {
    fun saveUrl(url: String) {
        settings.putString(KEY, url)
    }

    fun saveName(name: String) {
        settings.putString(KEY, name)
    }

    fun getName(name: String) {
        settings.putString(KEY, name)
    }

    fun getUrl(): String? = settings.getStringOrNull(KEY)

    private companion object {
        const val KEY = "server_url"
    }
}