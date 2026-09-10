package com.laschober.gymetrics.data.auth

import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.network.ConnectivityObserver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class SessionManager(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val settingStore: SettingStore,
    private val connectivityObserver: ConnectivityObserver
) {
    suspend fun resolveSession(): SessionState {
        if (tokenStore.accessToken() == null || tokenStore.refreshToken() == null) {
            return SessionState.NeedsLogin
        }

        if (!connectivityObserver.currentlyOnline()) return SessionState.Authenticated

        if (settingStore.getUrl() == null) return SessionState.NeedsServer

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