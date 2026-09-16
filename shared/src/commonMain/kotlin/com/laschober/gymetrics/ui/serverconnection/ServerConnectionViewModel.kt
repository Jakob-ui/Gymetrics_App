package com.laschober.gymetrics.ui.serverconnection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.remote.dto.ServerStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.coroutines.launch

class ServerConnectionViewModel(
    private val client: HttpClient,
    private val settingStore: SettingStore,
) : ViewModel() {

    var state: ServerConnectionState by mutableStateOf(ServerConnectionState.Idle)
        private set

    private sealed interface UrlCheck {
        data class Ok(val url: String) : UrlCheck
        data object HttpNotAllowed : UrlCheck
        data object BadFormat : UrlCheck
    }

    private fun validateUrl(input: String): UrlCheck {
        val trimmed = input.trim()
        return try {
            val withProtocol = if ("://" in trimmed) trimmed else "https://$trimmed"
            val parsed = Url(withProtocol)
            when {
                parsed.protocol.name == "http" -> UrlCheck.HttpNotAllowed
                parsed.host.isEmpty()          -> UrlCheck.BadFormat
                else                           -> UrlCheck.Ok(parsed.toString())
            }
        } catch (e: Exception) {
            UrlCheck.BadFormat
        }
    }

    fun checkConnection(input: String) {
        when (val check = validateUrl(input)) {
            UrlCheck.HttpNotAllowed ->
                state = ServerConnectionState.Error("Only HTTPS is allowed")

            UrlCheck.BadFormat ->
                state = ServerConnectionState.Error("That doesn't look like a valid address")

            is UrlCheck.Ok -> {
                val url = check.url
                state = ServerConnectionState.Loading
                viewModelScope.launch {
                    state = try {
                        val response = client.get("$url/status")
                        val status = response.body<ServerStatusDto>()
                        if (status.status == "ok" && "Gymetrics backend here" in status.message) {
                            settingStore.setAiMode(status.aiMode)
                            ServerConnectionState.Success(url, status.message)
                        } else {
                            ServerConnectionState.Error("Could not reach the server")
                        }
                    } catch (e: Exception) {
                        ServerConnectionState.Error("An error occurred")
                    }
                }
            }
        }
    }

    fun resetCheck() {
        when (state) {
            is ServerConnectionState.Success, is ServerConnectionState.Error ->
                state = ServerConnectionState.Idle
            else -> {}
        }
    }

    fun getSavedUrl(): String = settingStore.getUrl() ?: ""

    fun storeServerUrl() {
        val current = state
        if (current is ServerConnectionState.Success) {
            settingStore.saveUrl(current.url)
        }
    }
}
