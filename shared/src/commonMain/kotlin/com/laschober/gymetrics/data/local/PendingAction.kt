package com.laschober.gymetrics.data.local

import com.laschober.gymetrics.data.remote.dto.ExerciseDoneRequestDto
import kotlinx.serialization.Serializable

@Serializable
sealed interface PendingAction {
    val id: String

    @Serializable
    data class CreateTraining(
        override val id: String,
        val templateId: String,
        val activeDate: String,
    ) : PendingAction

    @Serializable
    data class DeleteTraining(
        override val id: String,
        val trainingId: String,
    ) : PendingAction

    @Serializable
    data class CompleteTraining(
        override val id: String,
        val trainingId: String,
        val plan: List<ExerciseDoneRequestDto>,
    ) : PendingAction
}
