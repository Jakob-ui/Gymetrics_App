package com.laschober.gymetrics.ui.main.logbook

import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto

sealed interface TrainingState {
    data object Loading : TrainingState
    data class Success(val trainings: List<TrainingOverviewResponseDto>) : TrainingState
    data class Error(val message: String) : TrainingState
}
