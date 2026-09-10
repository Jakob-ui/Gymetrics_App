package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class TrainingRepository(
    private val client: HttpClient,
    private val store: KStore<List<TrainingOverviewResponseDto>>,
    private val connectivityObserver: ConnectivityObserver,
) {

    suspend fun firstPage(
        limit: Int,
        asc: Boolean? = null,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        val isDefaultRequest = asc == null && active == null

        if (!isDefaultRequest) {
            check(connectivityObserver.isOnline.value) { "Offline - filter needs a connection" }
            val response = client.get("training") {
                parameter("page", 1)
                parameter("limit", limit)
                if (asc != null) parameter("asc", asc)
                if (active != null) parameter("active", active)
            }
            check(response.status.isSuccess()) { "Couldn't load trainings (${response.status.value})" }
            return response.body()
        }

        if (!connectivityObserver.isOnline.value) {
            return cachedOrThrow()
        }

        return try {
            val response = client.get("training") {
                parameter("page", 1)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                val list = response.body<List<TrainingOverviewResponseDto>>()
                store.set(list)
                list
            } else {
                cachedOrThrow()
            }
        } catch (e: NoCachedDataException) {
            throw e
        } catch (e: Exception) {
            println("trainings firstPage fetch failed, falling back to cache: $e")
            cachedOrThrow()
        }
    }

    private suspend fun cachedOrThrow(): List<TrainingOverviewResponseDto> {
        val cached = store.get() ?: emptyList()
        if (cached.isEmpty()) throw NoCachedDataException()
        return cached
    }    // Used for pull-to-refresh: unlike firstPage(), never silently falls back to the cache.


    // Used for pull-to-refresh: unlike firstPage(), never silently falls back to the cache.
    suspend fun refreshFirstPage(
        limit: Int,
        asc: Boolean? = null,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val response = client.get("training") {
            parameter("page", 1)
            parameter("limit", limit)
            if (asc != null) parameter("asc", asc)
            if (active != null) parameter("active", active)
        }
        check(response.status.isSuccess()) { "Couldn't load trainings (${response.status.value})" }
        val list = response.body<List<TrainingOverviewResponseDto>>()
        if (asc == null && active == null) store.set(list)
        return list
    }

    suspend fun fetchPage(
        page: Int,
        limit: Int,
        asc: Boolean? = null,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val response = client.get("training") {
            parameter("page", page)
            parameter("limit", limit)
            if (asc != null) parameter("asc", asc)
            if (active != null) parameter("active", active)
        }
        check(response.status.isSuccess()) { "Couldn't load trainings (${response.status.value})" }
        return response.body()
    }
}
