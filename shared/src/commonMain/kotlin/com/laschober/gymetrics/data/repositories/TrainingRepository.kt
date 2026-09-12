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
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val TRAINING_SORT_BY = "activeDate"

// The backend answers 404 "No Trainings found" for an empty result set instead of 200 + [] - this
// normalizes that (and any other empty-collection 404) to an empty list rather than an error, so
// e.g. deleting your last training doesn't leave a stale list stuck on screen forever.
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
            return response.toTrainingList()
        }

        // Cache-first: a cached snapshot is returned immediately without touching the network at
        // all. Pull-to-refresh (refreshFirstPage) is the explicit way to force a fresh fetch.
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

    // Cache-first, persisted across app restarts (unlike PlanningScreenViewModel's old in-memory
    // map). A month with zero trainings is cached as an empty list, distinct from "never fetched"
    // (a missing map entry) - both correctly avoid a refetch.
    suspend fun getTrainingsForMonth(year: Int, month: Int): List<TrainingOverviewResponseDto> {
        val key = monthKey(year, month)
        val cachedMonths = monthCacheStore.get() ?: emptyMap()
        cachedMonths[key]?.let { return it }

        check(connectivityObserver.isOnline.value) { "Offline - can't load planning data" }
        val response = client.get("training/monthlyTrainings") {
            parameter("year", year)
            parameter("month", month)
        }
        val list: List<TrainingOverviewResponseDto> = when {
            response.status == HttpStatusCode.NotFound -> emptyList()
            response.status.isSuccess() -> response.body()
            else -> error("Couldn't load trainings for $year-$month (${response.status.value})")
        }
        monthCacheStore.set(cachedMonths + (key to list))
        return list
    }

    // Called after creating/deleting a training so the affected month is re-fetched next time
    // instead of serving the now-stale cached list.
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

    // Offline: queued instead of failing outright. PlanningScreenViewModel's "Add training"
    // button closes either way, and it'll actually appear once SyncManager syncs it later.
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

    // Same offline queueing as createTraining.
    suspend fun deleteTraining(id: String) {
        if (!connectivityObserver.isOnline.value) {
            pendingActionQueue.enqueue(PendingAction.DeleteTraining(newActionId(), id))
            return
        }
        val response = client.delete("training/$id")
        check(response.status.isSuccess()) { "Couldn't delete training (${response.status.value})" }
        invalidateTrainingDetail(id)
        // The id alone doesn't say which month this training was in, so the whole month cache is
        // cleared rather than tracked - Planning just refetches whichever month it's next asked
        // to show.
        monthCacheStore.set(emptyMap())
    }

    // Cache-first, keyed by id. The backend returns [training, previousTraining?] - the second
    // element (for the "compare to last time" feature) isn't used yet, so only the requested
    // training itself is cached. That also means a cache hit here can't serve the comparison
    // feature once it's built - that'll need to bypass this cache or fetch separately.
    suspend fun getTraining(id: String): List<TrainingResponseDto> {
        detailStore.get()?.get(id)?.let { return listOf(it) }

        check(connectivityObserver.isOnline.value) { "Offline - can't load this training" }
        val response = client.get("training/$id")
        check(response.status.isSuccess()) { "Couldn't load training (${response.status.value})" }
        val result = response.body<List<TrainingResponseDto>>()
        result.firstOrNull()?.let { cacheTrainingDetail(it) }
        return result
    }

    // Called after the list's first page loads with the ids it already knows about, so opening
    // one of them later is instant. Best-effort and silent, and skips ids already cached - see
    // TemplateRepository.prefetchTemplateDetails for the same pattern.
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

    // Stale-while-revalidate, not cache-first: this always hits the network (Home needs it fresh -
    // it changes whenever a training is scheduled/deleted in Planning). getCachedNextTraining()
    // is only for seeding the UI with the last known answer instantly on a cold start, while this
    // fetches and overwrites it with the real, current answer right after.
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
