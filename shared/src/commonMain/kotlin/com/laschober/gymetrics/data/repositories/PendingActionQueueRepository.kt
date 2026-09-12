package com.laschober.gymetrics.data.repositories

import com.laschober.gymetrics.data.local.PendingAction
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PendingActionQueueRepository(private val store: KStore<List<PendingAction>>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _pendingActions = MutableStateFlow<List<PendingAction>>(emptyList())
    val pendingActions: StateFlow<List<PendingAction>> = _pendingActions.asStateFlow()

    init {
        scope.launch {
            _pendingActions.value = store.get() ?: emptyList()
        }
    }

    suspend fun enqueue(action: PendingAction) {
        val updated = _pendingActions.value + action
        _pendingActions.value = updated
        store.set(updated)
    }

    suspend fun remove(actionId: String) {
        val updated = _pendingActions.value.filterNot { it.id == actionId }
        _pendingActions.value = updated
        store.set(updated)
    }
}
