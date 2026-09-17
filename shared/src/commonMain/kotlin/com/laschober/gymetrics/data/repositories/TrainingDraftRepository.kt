package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.local.TrainingDraft
import io.github.xxfast.kstore.KStore

class TrainingDraftRepository(
    private val store: KStore<Map<String, TrainingDraft>>,
) {
    suspend fun getDraft(trainingId: String): TrainingDraft? = store.get()?.get(trainingId)

    suspend fun saveDraft(draft: TrainingDraft) {
        val current = store.get() ?: emptyMap()
        store.set(current + (draft.trainingId to draft))
    }

    suspend fun clearDraft(trainingId: String) {
        val current = store.get() ?: return
        if (trainingId in current) store.set(current - trainingId)
    }

    suspend fun clearCache() {
        store.set(emptyMap())
    }
}
