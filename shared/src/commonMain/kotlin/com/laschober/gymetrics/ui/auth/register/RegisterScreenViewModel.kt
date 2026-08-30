package com.laschober.gymetrics.ui.auth.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.core.network.HttpClientProvider
import com.laschober.gymetrics.data.local.TokenStore
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class RegisterScreenViewModel : ViewModel() {
    private val client = HttpClientProvider.client
    private val tokenStore = TokenStore()

    var state: RegisterState by mutableStateOf(RegisterState.Idle)
        private set

    @Serializable
    data class  RegisterRequestDto(
        val name: String,
        val email: String,
        val password: String,
    )

    @Serializable
    data class RegisterResponseDto(
        val userId : String,
        val name : String,
        val token : String,
        val refreshToken: String,
    )

    fun register(name: String, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            state = RegisterState.Error("Email and password required")
            return
        }
        state = RegisterState.Loading
        viewModelScope.launch {
            state = try {
                val response = client.post("auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequestDto(name, email, password))
                }
                println("register status = ${response.status}")

                when {
                    response.status.isSuccess() -> {
                        val auth = response.body<RegisterResponseDto>()
                        tokenStore.saveTokens(auth.token, auth.refreshToken)
                        RegisterState.Success
                    }
                    response.status == HttpStatusCode.Conflict ->
                        RegisterState.Error("Diese E-Mail ist bereits registriert")
                    else ->
                        RegisterState.Error("Registrierung fehlgeschlagen (${response.status.value})")
                }
            } catch (e: Exception) {
                println("Error $e")
                RegisterState.Error("Register failed")
            }
        }
    }
}
