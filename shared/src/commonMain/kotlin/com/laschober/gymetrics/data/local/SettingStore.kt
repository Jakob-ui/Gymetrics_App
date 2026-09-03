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

    private companion object {
        const val URLKEY = "server_url"
        const val NAMEKEY = "name"
    }
}