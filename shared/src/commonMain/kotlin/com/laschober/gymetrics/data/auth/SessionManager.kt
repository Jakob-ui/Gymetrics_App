package com.laschober.gymetrics.data.auth

import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class SessionManager(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val settingStore: SettingStore,
) {
    suspend fun resolveSession(): SessionState {
        if (settingStore.getUrl() == null) return SessionState.NeedsServer

        if (tokenStore.accessToken() == null || tokenStore.refreshToken() == null) {
            return SessionState.NeedsLogin
        }

        return try {
            val response = client.get("auth/validate")
            if (response.status.isSuccess()) SessionState.Authenticated
            else SessionState.NeedsLogin
        } catch (e: Exception) {
            println("session check failed: $e")
            SessionState.NeedsLogin
        }
    }

    fun logout() {
        tokenStore.clear()
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }
}