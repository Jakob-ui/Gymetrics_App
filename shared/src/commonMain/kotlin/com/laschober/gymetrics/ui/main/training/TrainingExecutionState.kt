package com.laschober.gymetrics.ui.main.training

import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto

sealed interface TrainingExecutionState {
    data object Loading : TrainingExecutionState
    data class Success(val training: TrainingResponseDto) : TrainingExecutionState
    data class Error(val message: String) : TrainingExecutionState
}
