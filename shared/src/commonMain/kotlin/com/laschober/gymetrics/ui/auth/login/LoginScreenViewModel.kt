package com.laschober.gymetrics.ui.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch

class LoginScreenViewModel (private val client : HttpClient,private val tokenStore : TokenStore) : ViewModel( ){
    var state: LoginState by mutableStateOf(LoginState.Idle)
        private set

    @Serializable
    data class  LoginRequestDto(
        val email: String,
        val password: String,
    )

    @Serializable
    data class LoginResponseDto(
        val userId : String,
        val name : String,
        val token : String,
        val refreshToken: String,
    )

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
                        val auth = response.body<LoginResponseDto>()
                        tokenStore.saveTokens(auth.token, auth.refreshToken)
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
