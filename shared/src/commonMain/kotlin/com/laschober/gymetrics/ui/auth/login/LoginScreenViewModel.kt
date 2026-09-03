package com.laschober.gymetrics.ui.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.remote.dto.AuthResponseDto
import com.laschober.gymetrics.data.remote.dto.LoginRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.launch

class LoginScreenViewModel(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val settingStore: SettingStore,
) : ViewModel() {

    var state: LoginState by mutableStateOf(LoginState.Idle)
        private set

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            state = LoginState.Error("Email and password required")
            return
        }
        state = LoginState.Loading
        viewModelScope.launch {
            state = try {
                val response = client.post("auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequestDto(email, password))
                }
                println("login status = ${response.status}")

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val auth = response.body<AuthResponseDto>()
                        tokenStore.saveTokens(auth.token, auth.refreshToken)
                        settingStore.saveName(auth.name)
                        LoginState.Success
                    }
                    HttpStatusCode.Unauthorized ->
                        LoginState.Error("Wrong E-Mail or password")
                    else ->
                        LoginState.Error("Login failed")
                }
            } catch (e: Exception) {
                println("Error $e")
                LoginState.Error("Login failed")
            }
        }
    }
}
