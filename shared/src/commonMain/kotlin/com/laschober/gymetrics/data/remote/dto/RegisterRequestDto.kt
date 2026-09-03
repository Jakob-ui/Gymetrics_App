package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

// Body for POST /auth/register.
@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
)
