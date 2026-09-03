package com.laschober.gymetrics.ui.auth.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.remote.dto.AuthResponseDto
import com.laschober.gymetrics.data.remote.dto.RegisterRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch

class RegisterScreenViewModel(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
) : ViewModel() {

    var state: RegisterState by mutableStateOf(RegisterState.Idle)
        private set

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
                        val auth = response.body<AuthResponseDto>()
                        tokenStore.saveTokens(auth.token, auth.refreshToken)
                        RegisterState.Success
                    }
                    response.status == HttpStatusCode.Conflict ->
                        RegisterState.Error("This email is already registered")
                    else ->
                        RegisterState.Error("Registration failed (${response.status.value})")
                }
            } catch (e: Exception) {
                println("Error $e")
                RegisterState.Error("Register failed")
            }
        }
    }
}
