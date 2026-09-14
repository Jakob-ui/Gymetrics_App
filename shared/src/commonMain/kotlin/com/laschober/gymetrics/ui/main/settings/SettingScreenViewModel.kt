package com.laschober.gymetrics.ui.main.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.ThemeMode
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import com.laschober.gymetrics.data.remote.dto.UserUpdateRequestDto
import com.laschober.gymetrics.data.repositories.HomeRepository
import com.laschober.gymetrics.data.repositories.ThemeModeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingScreenViewModel(
    private val client: HttpClient,
    private val homeRepository: HomeRepository,
    private val themeModeRepository: ThemeModeRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeModeRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        themeModeRepository.setThemeMode(mode)
    }

    // Only needed here to know the current studio and to keep the other profile fields intact
    // when saving - the backend's PUT /user requires all six fields every time, same as
    // ProfileScreenViewModel's save().
    var profile: UserProfileDto? by mutableStateOf(null)
        private set

    var studioSaving: Boolean by mutableStateOf(false)
        private set

    var studioError: String? by mutableStateOf(null)
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = client.get("user/profile")
                if (response.status.isSuccess()) {
                    profile = response.body<UserProfileDto>()
                }
            } catch (e: Exception) {
                println("settings: loading profile failed: $e")
            }
        }
    }

    fun setStudio(studio: String) {
        val current = profile ?: return
        if (studioSaving) return
        studioSaving = true
        studioError = null
        viewModelScope.launch {
            try {
                val response = client.put("user") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UserUpdateRequestDto(
                            name = current.name,
                            gender = current.gender,
                            height = current.height,
                            weight = current.weight,
                            muscle = current.muscle,
                            activeStudio = studio,
                        ),
                    )
                }
                if (response.status.isSuccess()) {
                    val updated = current.copy(activeStudio = studio)
                    profile = updated
                    homeRepository.cacheProfile(updated)
                } else {
                    studioError = "Couldn't save studio (${response.status.value})"
                }
            } catch (e: Exception) {
                println("settings: saving studio failed: $e")
                studioError = "Network error"
            } finally {
                studioSaving = false
            }
        }
    }
}
