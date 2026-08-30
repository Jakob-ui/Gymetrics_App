package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val userId: String,
    val name: String,
    val token: String,
    val refreshToken: String,
)
