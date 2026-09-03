package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

// Response of GET /status - used to confirm we are talking to a Gymetrics backend.
@Serializable
data class ServerStatusDto(
    val status: String,
    val message: String,
    val timestamp: String,
)
