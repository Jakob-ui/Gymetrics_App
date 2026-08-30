package com.laschober.gymetrics.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.url
import com.laschober.gymetrics.data.local.ServerUrlStore

object HttpClientProvider {

    val client: HttpClient = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation){
            json(Json { ignoreUnknownKeys = true })
        }
        install(DefaultRequest) {
            val base = ServerUrlStore().get()
            if (base != null) {
                url(if (base.endsWith("/")) base else "$base/")
            }
            }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
        }
    }
}
