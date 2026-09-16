package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenerateTemplateRequestDto(
    val studio: String,
    val message: String? = null,
)
