package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateRequestDto(
    val name: String,
    val gender: String,
    val height: String,
    val weight: String,
    val muscle: String,
    val activeStudio: String,
)
