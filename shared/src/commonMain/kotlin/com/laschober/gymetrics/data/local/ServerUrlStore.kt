package com.laschober.gymetrics.data.local

import com.russhwolf.settings.Settings

class ServerUrlStore(
    private val settings: Settings = Settings(),
) {
    fun save(url: String) {
        settings.putString(KEY, url)
    }

    fun get(): String? = settings.getStringOrNull(KEY)

    private companion object {
        const val KEY = "server_url"
    }
}