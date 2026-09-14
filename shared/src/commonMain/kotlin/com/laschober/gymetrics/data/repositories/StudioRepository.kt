package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.remote.dto.CitiesResponseDto
import com.laschober.gymetrics.data.remote.dto.CountriesResponseDto
import com.laschober.gymetrics.data.remote.dto.StudiosResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
class StudioRepository(
    private val client: HttpClient,
) {
    suspend fun getCountries(): List<String> {
        val response = client.get("studios")
        check(response.status.isSuccess()) { "Couldn't load countries (${response.status.value})" }
        return response.body<CountriesResponseDto>().countries
    }

    suspend fun getCities(country: String): List<String> {
        val response = client.get("studios/cities") { parameter("country", country) }
        check(response.status.isSuccess()) { "Couldn't load cities (${response.status.value})" }
        return response.body<CitiesResponseDto>().cities
    }

    suspend fun getStudios(country: String, city: String): List<String> {
        val response = client.get("studios/gyms") {
            parameter("country", country)
            parameter("city", city)
        }
        check(response.status.isSuccess()) { "Couldn't load studios (${response.status.value})" }
        return response.body<StudiosResponseDto>().studios
    }
}
