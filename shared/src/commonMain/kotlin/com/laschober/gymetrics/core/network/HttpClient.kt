package com.laschober.gymetrics.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.DefaultRequest
import com.laschober.gymetrics.data.local.ServerUrlStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.data.remote.dto.AuthResponseDto
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

fun buildHttpClient(serverUrlStore: ServerUrlStore, tokenStore: TokenStore) : HttpClient = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation){
            json(Json { ignoreUnknownKeys = true })
        }
    install(Auth) {
        bearer {
            // Try request if tokens are here
            loadTokens {
                val access = tokenStore.accessToken()
                val refresh = tokenStore.refreshToken()
                if (access != null && refresh != null) BearerTokens(access, refresh) else null
            }

            // try refresh
            refreshTokens {
                val refresh = tokenStore.refreshToken() ?: return@refreshTokens null
                val response = client.post("auth/refresh") {
                    markAsRefreshTokenRequest()                          // keep this call out of the retry loop
                    header(HttpHeaders.Authorization, "Bearer $refresh")
                }
                if (!response.status.isSuccess()) {
                    tokenStore.clear()
                    return@refreshTokens null
                }
                val auth = response.body<AuthResponseDto>()
                tokenStore.saveTokens(auth.token, auth.refreshToken)
                BearerTokens(auth.token, auth.refreshToken)
            }

            sendWithoutRequest { true }
        }
    }
        install(DefaultRequest) {
            val base = serverUrlStore.get()
            if (base != null) {
                url(if (base.endsWith("/")) base else "$base/")
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
        }
}