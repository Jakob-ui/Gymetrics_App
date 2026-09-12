package com.laschober.gymetrics.data.local

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
}
