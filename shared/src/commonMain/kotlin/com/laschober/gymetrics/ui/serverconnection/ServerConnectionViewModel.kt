package com.laschober.gymetrics.ui.serverconnection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.core.network.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch
import io.ktor.http.Url
import kotlinx.serialization.Serializable

class ServerConnectionViewModel : ViewModel() {
    private val client = HttpClientProvider.client
    var state : ServerConnectionState by mutableStateOf(ServerConnectionState.Idle)

    private sealed interface UrlCheck {
        data class Ok(val url: String) : UrlCheck
        data object HttpNotAllowed : UrlCheck
        data object BadFormat : UrlCheck
    }

    @Serializable
    data class  ServerStatusDto(
        val status: String,
        val message: String,
        val timestamp: String,
    )

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
                state = ServerConnectionState.Loading
                viewModelScope.launch {
                    state = try {
                        val response = client.get(check.url)
                        val status = response.body<ServerStatusDto>()
                        if (status.status == "ok" && "Gymetrics backend here" in status.message){
                            ServerConnectionState.Success(status.message)
                        } else {
                            ServerConnectionState.Error("Could not reach the server")
                        }
                    } catch (e: Exception) {
                        ServerConnectionState.Error("An error occured")
                    }
                }
            }
        }
    }

    fun connectToServer(){}
}