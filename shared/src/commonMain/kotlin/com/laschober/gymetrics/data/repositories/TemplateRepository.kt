package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.network.ConnectivityObserver
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
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class NoCachedDataException : Exception("No cached templates")

// Mirrors the backend's TemplateSortBy enum (template.query.dto.ts).
enum class TemplateSortBy(val apiValue: String) {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
}

class TemplateRepository (private val client: HttpClient,
                          private val store: KStore<List<TemplateOverviewResponseDto>>,
                          private val connectivityObserver: ConnectivityObserver) {

    // asc defaults to false = newest first. We always send it explicitly rather than relying on
    // the backend's default (which is still asc=true on the deployed instance).
    suspend fun firstPage(
        limit: Int,
        search: String? = null,
        sortBy: TemplateSortBy? = null,
        asc: Boolean = false,
    ): List<TemplateOverviewResponseDto> {
        val query = search?.trim()?.takeIf { it.isNotBlank() }
        // The default, cacheable view: no search, default sort field, newest first.
        val isDefaultRequest = query == null && sortBy == null && !asc

        // A search or a non-default sort is a plain network-only call, same as fetchPage - no
        // cache fallback, because the only thing we ever cache is the plain default-sorted
        // unfiltered first page, which wouldn't match a different query or order.
        if (!isDefaultRequest) {
            check(connectivityObserver.isOnline.value) { "Offline - search/sort needs a connection" }
            val response = client.get("templates") {
                parameter("page", 1)
                parameter("limit", limit)
                parameter("asc", asc)
                if (query != null) parameter("search", query)
                if (sortBy != null) parameter("sortBy", sortBy.apiValue)
            }
            check(response.status.isSuccess()) { "Couldn't load templates (${response.status.value})" }
            return response.body()
        }

        if (!connectivityObserver.isOnline.value) {
            return cachedOrThrow()
        }

        return try {
            val response = client.get("templates") {
                parameter("page", 1)
                parameter("limit", limit)
                parameter("asc", false)
            }
            if (response.status.isSuccess()) {
                val list = response.body<List<TemplateOverviewResponseDto>>()
                store.set(list)
                list
            } else {
                cachedOrThrow()
            }
        } catch (e: NoCachedDataException) {
            throw e
        } catch (e: Exception) {
            println("templates firstPage fetch failed, falling back to cache: $e")
            cachedOrThrow()
        }
    }

    private suspend fun cachedOrThrow(): List<TemplateOverviewResponseDto> {
        val cached = store.get() ?: emptyList()
        if (cached.isEmpty()) throw NoCachedDataException()
        return cached
    }

    // Used for pull-to-refresh: unlike firstPage(), never silently falls back to the cache -
    // the caller wants to know if the refresh itself failed.
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
        check(response.status.isSuccess()) { "Couldn't load templates (${response.status.value})" }
        val list = response.body<List<TemplateOverviewResponseDto>>()
        // Only cache the plain default view - caching search/sort results would corrupt the cache.
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
        check(response.status.isSuccess()) { "Couldn't load templates (${response.status.value})" }
        return response.body()
    }

    suspend fun getTemplate(id: String): TemplateResponseDto {
        check(connectivityObserver.isOnline.value) { "Offline - template details aren't cached" }
        val response = client.get("templates/$id")
        check(response.status.isSuccess()) { "Couldn't load template (${response.status.value})" }
        return response.body()
    }

    // Note: POST /templates responds with the raw document (`_id`, `_createdAt`, ...), a different
    // shape than TemplateResponseDto (`id`, `created_date`, ...) used by GET /templates/:id and PUT.
    // We don't need the body here - the screen already has everything it needs locally - so we
    // just check the status and ignore it, instead of adding a DTO just to decode a response we throw away.
    suspend fun createTemplate(request: TemplateRequestDto) {
        check(connectivityObserver.isOnline.value) { "Offline - can't create a template" }
        val response = client.post("templates") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Couldn't create template (${response.status.value})" }
    }

    suspend fun updateTemplate(id: String, request: TemplateRequestDto) {
        check(connectivityObserver.isOnline.value) { "Offline - can't update a template" }
        val response = client.put("templates/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Couldn't update template (${response.status.value})" }
    }

    suspend fun deleteTemplate(id: String) {
        check(connectivityObserver.isOnline.value) { "Offline - can't delete a template" }
        val response = client.delete("templates/$id")
        check(response.status.isSuccess()) { "Couldn't delete template (${response.status.value})" }
    }
}