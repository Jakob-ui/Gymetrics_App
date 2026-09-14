package com.laschober.gymetrics.data.sync

import com.laschober.gymetrics.data.local.PendingAction
import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.repositories.PendingActionQueueRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncManager(
    private val connectivityObserver: ConnectivityObserver,
    private val queueRepository: PendingActionQueueRepository,
    private val trainingRepository: TrainingRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val pendingCount: StateFlow<Int> = queueRepository.pendingActions
        .map { it.size }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        scope.launch {
            connectivityObserver.isOnline.collect { online ->
                if (online) trySyncAll()
            }
        }
    }

    private suspend fun trySyncAll() {
        while (true) {
            if (!connectivityObserver.isOnline.value) return
            val next = queueRepository.pendingActions.value.firstOrNull() ?: return
            val succeeded = try {
                execute(next)
                true
            } catch (e: Exception) {
                println("sync: action ${next.id} failed, will retry once back online: $e")
                false
            }
            if (succeeded) queueRepository.remove(next.id) else return
        }
    }

    private suspend fun execute(action: PendingAction) {
        when (action) {
            is PendingAction.CreateTraining -> trainingRepository.createTraining(action.templateId, action.activeDate)
            is PendingAction.DeleteTraining -> trainingRepository.deleteTraining(action.trainingId)
            is PendingAction.CompleteTraining -> trainingRepository.completeTraining(action.trainingId)
        }
    }
}
