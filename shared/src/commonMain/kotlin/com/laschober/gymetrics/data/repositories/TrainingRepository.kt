package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.local.NextTrainingCache
import com.laschober.gymetrics.data.local.PendingAction
import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingRequestDto
import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto
import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val TRAINING_SORT_BY = "activeDate"

private suspend fun HttpResponse.toTrainingList(): List<TrainingOverviewResponseDto> = when {
    status == HttpStatusCode.NotFound -> emptyList()
    status.isSuccess() -> body()
    else -> error("Couldn't load trainings (${status.value})")
}

class TrainingRepository(
    private val client: HttpClient,
    private val store: KStore<List<TrainingOverviewResponseDto>>,
    private val monthCacheStore: KStore<Map<String, List<TrainingOverviewResponseDto>>>,
    private val detailStore: KStore<Map<String, TrainingResponseDto>>,
    private val nextTrainingCacheStore: KStore<List<NextTrainingCache>>,
    private val pendingActionQueue: PendingActionQueueRepository,
    private val connectivityObserver: ConnectivityObserver,
) {

    suspend fun firstPage(
        limit: Int,
        asc: Boolean = false,
        active: Boolean? = null,
    ): List<TrainingOverviewResponseDto> {
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
            return response.toTrainingList()
        }

        val cached = store.get()
        if (!cached.isNullOrEmpty()) return cached

        if (!connectivityObserver.isOnline.value) {
            throw NoCachedDataException()
        }

        val response = client.get("training") {
            parameter("page", 1)
            parameter("limit", limit)
            parameter("sortBy", TRAINING_SORT_BY)
            parameter("asc", false)
        }
        val list = response.toTrainingList()
        store.set(list)
        return list
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
        val list = response.toTrainingList()
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
        return response.toTrainingList()
    }

    // The backend answers this endpoint with the full detail shape (_id, plan, ...) rather than
    // the overview one, so this deserializes as TrainingResponseDto and maps down to the overview
    // shape that the rest of Planning (state, cache, UI) works with everywhere else.
    suspend fun getTrainingsForMonth(year: Int, month: Int): List<TrainingOverviewResponseDto> {
        val key = monthKey(year, month)
        val cachedMonths = monthCacheStore.get() ?: emptyMap()
        cachedMonths[key]?.let { return it }

        check(connectivityObserver.isOnline.value) { "Offline - can't load planning data" }
        val response = client.get("training/monthlyTrainings") {
            parameter("year", year)
            parameter("month", month)
        }
        val details: List<TrainingResponseDto> = when {
            response.status == HttpStatusCode.NotFound -> emptyList()
            response.status.isSuccess() -> response.body()
            else -> error("Couldn't load trainings for $year-$month (${response.status.value})")
        }

        // Free bonus: since the full detail already came through, warm the per-id detail cache
        // with it too, instead of throwing that data away.
        if (details.isNotEmpty()) {
            val currentDetails = detailStore.get() ?: emptyMap()
            detailStore.set(currentDetails + details.associateBy { it.id })
        }

        val list = details.map { it.toOverview() }
        monthCacheStore.set(cachedMonths + (key to list))
        return list
    }

    private fun TrainingResponseDto.toOverview() = TrainingOverviewResponseDto(
        id = id,
        title = title,
        description = description,
        status = active,
        icon = icon,
        activeDate = activeDate,
        createdDate = createdAt,
        updatedDate = updatedAt,
    )

    suspend fun invalidateMonthCache(year: Int, month: Int) {
        val cachedMonths = monthCacheStore.get() ?: return
        val key = monthKey(year, month)
        if (key in cachedMonths) monthCacheStore.set(cachedMonths - key)
    }

    private fun monthKey(year: Int, month: Int) = "$year-$month"

    private fun parseYearMonth(activeDate: String): Pair<Int, Int>? {
        val parts = activeDate.split("-")
        if (parts.size < 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        return year to month
    }

    suspend fun createTraining(templateId: String, activeDate: String) {
        if (!connectivityObserver.isOnline.value) {
            pendingActionQueue.enqueue(PendingAction.CreateTraining(newActionId(), templateId, activeDate))
            return
        }
        val response = client.post("training/create") {
            contentType(ContentType.Application.Json)
            setBody(TrainingRequestDto(templateId = templateId, activeDate = activeDate))
        }
        check(response.status.isSuccess()) { "Couldn't create training (${response.status.value})" }
        parseYearMonth(activeDate)?.let { (year, month) -> invalidateMonthCache(year, month) }
    }

    suspend fun deleteTraining(id: String) {
        if (!connectivityObserver.isOnline.value) {
            pendingActionQueue.enqueue(PendingAction.DeleteTraining(newActionId(), id))
            return
        }
        val response = client.delete("training/$id")
        check(response.status.isSuccess()) { "Couldn't delete training (${response.status.value})" }
        invalidateTrainingDetail(id)
        monthCacheStore.set(emptyMap())
    }

    suspend fun completeTraining(id: String) {
        if (!connectivityObserver.isOnline.value) {
            pendingActionQueue.enqueue(PendingAction.CompleteTraining(newActionId(), id))
            return
        }
        val response = client.put("training/$id")
        check(response.status.isSuccess()) { "Couldn't complete training (${response.status.value})" }
        invalidateTrainingDetail(id)
        store.set(emptyList())
        monthCacheStore.set(emptyMap())
        nextTrainingCacheStore.set(emptyList())
    }

    suspend fun getTraining(id: String): List<TrainingResponseDto> {
        detailStore.get()?.get(id)?.let { return listOf(it) }

        check(connectivityObserver.isOnline.value) { "Offline - can't load this training" }
        val response = client.get("training/$id")
        check(response.status.isSuccess()) { "Couldn't training (${response.status.value})" }
        val result = response.body<List<TrainingResponseDto>>()
        result.firstOrNull()?.let { cacheTrainingDetail(it) }
        return result
    }

    suspend fun prefetchTrainingDetails(ids: List<String>) {
        val cached = detailStore.get() ?: emptyMap()
        val missing = ids.filterNot { it in cached }
        if (missing.isEmpty() || !connectivityObserver.isOnline.value) return

        var updated = cached
        for (id in missing) {
            try {
                val response = client.get("training/$id")
                if (response.status.isSuccess()) {
                    val result = response.body<List<TrainingResponseDto>>()
                    result.firstOrNull()?.let { updated = updated + (it.id to it) }
                }
            } catch (e: Exception) {
                println("training detail prefetch failed for $id: $e")
            }
        }
        detailStore.set(updated)
    }

    private suspend fun cacheTrainingDetail(training: TrainingResponseDto) {
        val current = detailStore.get() ?: emptyMap()
        detailStore.set(current + (training.id to training))
    }

    private suspend fun invalidateTrainingDetail(id: String) {
        val current = detailStore.get() ?: return
        if (id in current) detailStore.set(current - id)
    }

    suspend fun getNextTraining(): TrainingOverviewResponseDto? {
        check(connectivityObserver.isOnline.value) { "Offline - can't load your next training" }
        val response = client.get("training/nextTraining")
        val result: TrainingOverviewResponseDto? = when {
            response.status == HttpStatusCode.NotFound -> null
            response.status.isSuccess() -> response.body()
            else -> error("Couldn't load next training (${response.status.value})")
        }
        nextTrainingCacheStore.set(listOf(NextTrainingCache(result)))
        return result
    }

    suspend fun getCachedNextTraining(): NextTrainingCache? = nextTrainingCacheStore.get()?.firstOrNull()

    private fun newActionId(): String = kotlin.random.Random.nextLong().toString()
}
