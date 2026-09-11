package com.laschober.gymetrics.ui.main.planning

import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import kotlinx.datetime.LocalDate

sealed interface PlanningState {
    data object Loading : PlanningState
    data class Success(val trainingsByDate: Map<LocalDate, List<TrainingOverviewResponseDto>>) : PlanningState
    data class Error(val message: String) : PlanningState
}
