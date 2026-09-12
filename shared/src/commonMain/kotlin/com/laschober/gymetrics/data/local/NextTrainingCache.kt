package com.laschober.gymetrics.data.local

import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import kotlinx.serialization.Serializable

// Wraps the (possibly null) result so the cache can tell "nothing scheduled" (training == null,
// but still a real, cached answer) apart from "never fetched at all" (no entry in the store).
@Serializable
data class NextTrainingCache(val training: TrainingOverviewResponseDto?)
