package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.remote.dto.GenerateTemplateRequestDto
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TemplateRequestDto
import com.laschober.gymetrics.data.remote.dto.TemplateResponseDto
import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NoCachedDataException : Exception("No cached templates")

enum class TemplateSortBy(val apiValue: String) {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
}

private suspend fun HttpResponse.toTemplateList(): List<TemplateOverviewResponseDto> = when {
    status == HttpStatusCode.NotFound -> emptyList()
    status.isSuccess() -> body()
    else -> error("Couldn't load templates (${status.value})")
}

class TemplateRepository(
    private val client: HttpClient,
    private val store: KStore<List<TemplateOverviewResponseDto>>,
    private val detailStore: KStore<Map<String, TemplateResponseDto>>,
    private val connectivityObserver: ConnectivityObserver,
) {

    suspend fun firstPage(
        limit: Int,
        search: String? = null,
        sortBy: TemplateSortBy? = null,
        asc: Boolean = false,
    ): List<TemplateOverviewResponseDto> {
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        val isDefaultRequest = query == null && sortBy == null && !asc

        if (!isDefaultRequest) {
            check(connectivityObserver.isOnline.value) { "Offline - search/sort needs a connection" }
            val response = client.get("templates") {
                parameter("page", 1)
                parameter("limit", limit)
                parameter("asc", asc)
                if (query != null) parameter("search", query)
                if (sortBy != null) parameter("sortBy", sortBy.apiValue)
            }
            return response.toTemplateList()
        }

        val cached = store.get()
        if (!cached.isNullOrEmpty()) return cached

        if (!connectivityObserver.isOnline.value) {
            throw NoCachedDataException()
        }

        val response = client.get("templates") {
            parameter("page", 1)
            parameter("limit", limit)
            parameter("asc", false)
        }
        val list = response.toTemplateList()
        store.set(list)
        return list
    }

    suspend fun refreshFirstPage(
        limit: Int,
        search: String? = null,
        sortBy: TemplateSortBy? = null,
        asc: Boolean = false,
    ): List<TemplateOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        val response = client.get("templates") {
            parameter("page", 1)
            parameter("limit", limit)
            parameter("asc", asc)
            if (query != null) parameter("search", query)
            if (sortBy != null) parameter("sortBy", sortBy.apiValue)
        }
        val list = response.toTemplateList()
        if (query == null && sortBy == null && !asc) store.set(list)
        return list
    }

    suspend fun fetchPage(
        page: Int,
        limit: Int,
        search: String? = null,
        sortBy: TemplateSortBy? = null,
        asc: Boolean = false,
    ): List<TemplateOverviewResponseDto> {
        check(connectivityObserver.isOnline.value) { "Offline - skipping network call" }
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        val response = client.get("templates") {
            parameter("page", page)
            parameter("limit", limit)
            parameter("asc", asc)
            if (query != null) parameter("search", query)
            if (sortBy != null) parameter("sortBy", sortBy.apiValue)
        }
        return response.toTemplateList()
    }

    suspend fun getTemplate(id: String): TemplateResponseDto {
        detailStore.get()?.get(id)?.let { return it }

        check(connectivityObserver.isOnline.value) { "Offline - template details aren't cached" }
        val response = client.get("templates/$id")
        check(response.status.isSuccess()) { "Couldn't load template (${response.status.value})" }
        val template = response.body<TemplateResponseDto>()
        cacheTemplateDetail(template)
        return template
    }

    suspend fun prefetchTemplateDetails(ids: List<String>) {
        val cached = detailStore.get() ?: emptyMap()
        val missing = ids.filterNot { it in cached }
        if (missing.isEmpty() || !connectivityObserver.isOnline.value) return

        var updated = cached
        for (id in missing) {
            try {
                val response = client.get("templates/$id")
                if (response.status.isSuccess()) {
                    val template = response.body<TemplateResponseDto>()
                    updated = updated + (template.id to template)
                }
            } catch (e: Exception) {
                println("template detail prefetch failed for $id: $e")
            }
        }
        detailStore.set(updated)
    }

    private suspend fun cacheTemplateDetail(template: TemplateResponseDto) {
        val current = detailStore.get() ?: emptyMap()
        detailStore.set(current + (template.id to template))
    }

    private suspend fun invalidateTemplateDetail(id: String) {
        val current = detailStore.get() ?: return
        if (id in current) detailStore.set(current - id)
    }

    suspend fun createTemplate(request: TemplateRequestDto) {
        check(connectivityObserver.isOnline.value) { "Offline - can't create a template" }
        val response = client.post("templates") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Couldn't create template (${response.status.value})" }
        store.set(emptyList())
    }

    suspend fun generateTemplateWithAi(studio: String, message: String?): TemplateResponseDto {
        check(connectivityObserver.isOnline.value) { "Offline - can't generate a template with AI" }
        val response = client.post("templates/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateTemplateRequestDto(studio = studio, message = message))
        }
        check(response.status.isSuccess()) { "Couldn't start AI generation (${response.status.value})" }
        val placeholder = response.body<TemplateResponseDto>()
        store.set(emptyList())
        return placeholder
    }

    suspend fun updateTemplate(id: String, request: TemplateRequestDto) {
        check(connectivityObserver.isOnline.value) { "Offline - can't update a template" }
        val response = client.put("templates/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Couldn't update template (${response.status.value})" }
        invalidateTemplateDetail(id)
        store.set(emptyList())
    }

    suspend fun deleteTemplate(id: String) {
        check(connectivityObserver.isOnline.value) { "Offline - can't delete a template" }
        val response = client.delete("templates/$id")
        check(response.status.isSuccess()) { "Couldn't delete template (${response.status.value})" }
        invalidateTemplateDetail(id)
        store.set(emptyList())
    }
}