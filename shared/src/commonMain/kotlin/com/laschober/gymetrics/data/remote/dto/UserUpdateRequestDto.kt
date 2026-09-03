package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

// Body for PUT /user. The backend requires all six fields (@IsString), so we
// always send the complete object - empty strings are allowed, missing keys are not.
@Serializable
data class UserUpdateRequestDto(
    val name: String,
    val gender: String,
    val height: String,
    val weight: String,
    val muscle: String,
    val activeStudio: String,
)
