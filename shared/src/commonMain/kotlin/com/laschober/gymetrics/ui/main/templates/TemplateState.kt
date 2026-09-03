package com.laschober.gymetrics.ui.main.templates

import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto

sealed interface TemplateState {
    data object Loading : TemplateState
    data class Success(val templates: List<TemplateOverviewResponseDto>) : TemplateState
    data class Error(val message: String) : TemplateState
}