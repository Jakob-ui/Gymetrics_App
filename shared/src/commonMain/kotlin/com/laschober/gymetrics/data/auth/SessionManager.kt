package com.laschober.gymetrics.data.auth

import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.network.ConnectivityObserver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class SessionManager(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val settingStore: SettingStore,
    private val connectivityObserver: ConnectivityObserver
) {
    suspend fun resolveSession(): SessionState {
        if (tokenStore.accessToken() == null || tokenStore.refreshToken() == null) {
            if(settingStore.getSetupStatus() == false){
                return SessionState.NeedsServer
            }
            return SessionState.NeedsLogin
        }

        if (!connectivityObserver.currentlyOnline()) return SessionState.Authenticated

        if (settingStore.getUrl() == null) return SessionState.NeedsServer

        return try {
            val response = client.get("auth/validate")
            if (response.status.isSuccess()) SessionState.Authenticated
            else {
                val refresh = tokenStore.refreshToken()
                val response = client.post("auth/refresh") {
                    header(HttpHeaders.Authorization, "Bearer $refresh")
                }
                if (response.status.isSuccess()){
                    SessionState.Authenticated
                } else SessionState.NeedsLogin
            }
        } catch (e: Exception) {
            println("session check failed: $e")
            SessionState.NeedsLogin
        }
    }

    fun logout() {
        settingStore.setAiMode(false)
        settingStore.toggleSetup(false)
        tokenStore.clear()
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }
}