package com.laschober.gymetrics.ui.main.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.auth.SessionManager
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import com.laschober.gymetrics.data.remote.dto.UserUpdateRequestDto
import com.laschober.gymetrics.data.repositories.HomeRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch

data class ProfileForm(
    val name: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val muscle: String = "",
    val activeStudio: String = "",
)

class ProfileScreenViewModel(
    private val client: HttpClient,
    private val sessionManager: SessionManager,
    private val homeRepository: HomeRepository,
    settingStore: SettingStore,
) : ViewModel() {

    val serverUrl: String = settingStore.getUrl().orEmpty()
    val aiMode: Boolean = settingStore.getAiMode() == true

    var state: ProfileState by mutableStateOf(ProfileState.Loading)
        private set

    var editing: Boolean by mutableStateOf(false)
        private set

    var form: ProfileForm by mutableStateOf(ProfileForm())
        private set

    var saving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    init { load() }

    fun load(showLoading: Boolean = true) {
        if (showLoading) state = ProfileState.Loading
        viewModelScope.launch {
            val next = try {
                val response = client.get("user/profile")
                if (response.status.isSuccess()) {
                    val profile = response.body<UserProfileDto>()
                    homeRepository.cacheProfile(profile)
                    ProfileState.Success(profile)
                } else {
                    ProfileState.Error("Couldn't load profile (${response.status.value})")
                }
            } catch (e: Exception) {
                println("profile load failed: $e")
                ProfileState.Error("Network error")
            }
            state = next
        }
    }

    fun startEditing() {
        val profile = (state as? ProfileState.Success)?.profile ?: return
        form = ProfileForm(
            name = profile.name,
            gender = profile.gender,
            height = profile.height,
            weight = profile.weight,
            muscle = profile.muscle,
            activeStudio = profile.activeStudio,
        )
        saveError = null
        editing = true
    }

    fun updateForm(new: ProfileForm) {
        form = new
    }

    fun cancelEditing() {
        editing = false
        saveError = null
    }

    fun save() {
        if (saving) return
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                val response = client.put("user") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UserUpdateRequestDto(
                            name = form.name,
                            gender = form.gender,
                            height = form.height,
                            weight = form.weight,
                            muscle = form.muscle,
                            activeStudio = form.activeStudio,
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    editing = false
                    load(showLoading = false)
                } else {
                    saveError = "Save failed (${response.status.value})"
                }
            } catch (e: Exception) {
                println("profile save failed: $e")
                saveError = "Network error"
            } finally {
                saving = false
            }
        }
    }

    fun logout() = sessionManager.logout()
}
