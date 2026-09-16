package com.laschober.gymetrics.data.local

import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class NextTrainingCache(val training: TrainingOverviewResponseDto?)
