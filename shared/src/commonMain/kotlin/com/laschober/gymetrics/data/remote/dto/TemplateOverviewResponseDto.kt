package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One item of GET /templates (the overview list - no exercise plan).
@Serializable
data class TemplateOverviewResponseDto(
    val id: String,
    val title: String,
    val description: String = "",
    val status: Boolean = false,
    val icon: String = "",
    @SerialName("created_date") val createdDate: String = "",
    @SerialName("updated_date") val updatedDate: String = "",
)
