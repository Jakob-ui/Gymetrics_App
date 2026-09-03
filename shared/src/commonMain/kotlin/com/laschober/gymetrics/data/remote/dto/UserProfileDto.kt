package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val name: String,
    val email: String,
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val muscle: String = "",
    val activeStudio: String = "",
)