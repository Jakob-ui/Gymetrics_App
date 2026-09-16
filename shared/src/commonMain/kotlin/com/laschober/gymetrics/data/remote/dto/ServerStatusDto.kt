package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServerStatusDto(
    val status: String,
    val message: String,
    val timestamp: String,
    val aiMode: Boolean,
)
