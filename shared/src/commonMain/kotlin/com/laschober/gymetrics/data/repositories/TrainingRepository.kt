package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

// The Logbook is always sorted by the training's scheduled date, never by when the record was
// created. `asc` still toggles the direction; `sortBy` is fixed.
private const val TRAINING_SORT_BY = "activeDate"

class TrainingRepository(
    private val client: HttpClient,
    private val store: KStore<List<TrainingOverviewResponseDto>>,
    private val connectivityObserver: ConnectivityObserver,
) {

    // asc defaults to false = newest scheduled date first. Always sent explicitly rather than
    // relying on the backend default (still asc=true on the deployed instance).
    suspend fun firstPage(
        limit: Int,
        asc: Boolean = false,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        // The default, cacheable view: newest first, no active filter.
        val isDefaultRequest = !asc && active == null

        if (!isDefaultRequest) {
            check(connectivityObserver.isOnline.value) { "Offline - filter needs a connection" }
            val response = client.get("training") {
                parameter("page", 1)
                parameter("limit", limit)
                parameter("sortBy", TRAINING_SORT_BY)
                parameter("asc", asc)
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
                parameter("sortBy", TRAINING_SORT_BY)
                parameter("asc", false)
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
    }


    suspend fun refreshFirstPage(
        limit: Int,
        asc: Boolean = false,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val response = client.get("training") {
            parameter("page", 1)
            parameter("limit", limit)
            parameter("sortBy", TRAINING_SORT_BY)
            parameter("asc", asc)
            if (active != null) parameter("active", active)
        }
        check(response.status.isSuccess()) { "Couldn't load trainings (${response.status.value})" }
        val list = response.body<List<TrainingOverviewResponseDto>>()
        if (!asc && active == null) store.set(list)
        return list
    }

    suspend fun fetchPage(
        page: Int,
        limit: Int,
        asc: Boolean = false,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val response = client.get("training") {
            parameter("page", page)
            parameter("limit", limit)
            parameter("sortBy", TRAINING_SORT_BY)
            parameter("asc", asc)
            if (active != null) parameter("active", active)
        }
        check(response.status.isSuccess()) { "Couldn't load trainings (${response.status.value})" }
        return response.body()
    }
}
