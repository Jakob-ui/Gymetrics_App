package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class HomeRepository(
    private val client: HttpClient,
    private val profileStore: KStore<List<UserProfileDto>>,
) {
    suspend fun getGreetingName(): String {
        profileStore.get()?.firstOrNull()?.let { return it.name }

        val response = client.get("user/profile")
        if (!response.status.isSuccess()) return ""
        val profile = response.body<UserProfileDto>()
        profileStore.set(listOf(profile))
        return profile.name
    }

    suspend fun cacheProfile(profile: UserProfileDto) {
        profileStore.set(listOf(profile))
    }

    suspend fun clearCache() {
        profileStore.set(emptyList())
    }
}
