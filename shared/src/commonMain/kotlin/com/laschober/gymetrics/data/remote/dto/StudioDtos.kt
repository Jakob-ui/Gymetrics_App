package com.laschober.gymetrics.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CountriesResponseDto(val countries: List<String> = emptyList())

@Serializable
data class CitiesResponseDto(val cities: List<String> = emptyList())

@Serializable
data class StudiosResponseDto(val studios: List<String> = emptyList())
