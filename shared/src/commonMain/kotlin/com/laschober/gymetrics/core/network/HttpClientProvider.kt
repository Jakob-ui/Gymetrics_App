package com.laschober.gymetrics.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider {

    val client: HttpClient = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation){
            json(Json { ignoreUnknownKeys = true })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 10_000
        }
    }
}
