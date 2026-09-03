package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

// Body for POST /auth/login.
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)
