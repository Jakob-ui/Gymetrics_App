package com.laschober.gymetrics.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn


expect fun observeConnectivity(): Flow<Boolean>

class ConnectivityObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    suspend fun currentlyOnline(): Boolean = observeConnectivity().first()
    val isOnline: StateFlow<Boolean> = observeConnectivity()
        .stateIn(scope, SharingStarted.Eagerly, initialValue = true)
}