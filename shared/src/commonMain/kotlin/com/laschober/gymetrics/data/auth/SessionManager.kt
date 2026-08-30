package com.laschober.gymetrics.data.auth

import com.laschober.gymetrics.core.network.HttpClientProvider
import com.laschober.gymetrics.data.local.ServerUrlStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.remote.dto.AuthResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class SessionManager(
    private val client: HttpClient = HttpClientProvider.client,
    private val tokenStore: TokenStore = TokenStore(),
    private val serverUrlStore: ServerUrlStore = ServerUrlStore(),
) {

    // Runs once at app startup. Never throws – always returns a decision.
    suspend fun resolveSession(): SessionState {
        if (serverUrlStore.get() == null) return SessionState.NeedsServer

        val access = tokenStore.accessToken()
        val refresh = tokenStore.refreshToken()
        if (access == null || refresh == null) return SessionState.NeedsLogin

        return try {
            when {
                validateAccessToken(access) -> SessionState.Authenticated
                refreshTokens(refresh) -> SessionState.Authenticated
                else -> {
                    tokenStore.clear()
                    SessionState.NeedsLogin
                }
            }
        } catch (e: Exception) {
            // Server unreachable etc. – we can't confirm the session, so play it
            // safe and send the user to login. (Later: allow offline with cache.)
            println("session check failed: $e")
            SessionState.NeedsLogin
        }
    }

    private suspend fun validateAccessToken(accessToken: String): Boolean {
        val response = client.get("auth/validate") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return response.status.isSuccess()
    }

    private suspend fun refreshTokens(refreshToken: String): Boolean {
        val response = client.post("auth/refresh") {
            header(HttpHeaders.Authorization, "Bearer $refreshToken")
        }
        if (!response.status.isSuccess()) return false

        val auth = response.body<AuthResponseDto>()
        tokenStore.saveTokens(auth.token, auth.refreshToken)
        return true
    }

    public fun logout() {
        tokenStore.clear()
    }
}
