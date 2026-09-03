package com.laschober.gymetrics.ui.main.profile

import com.laschober.gymetrics.data.remote.dto.UserProfileDto

sealed interface ProfileState {
    data object Loading : ProfileState
    data class Success(val profile: UserProfileDto) : ProfileState
    data class Error(val message: String) : ProfileState
}