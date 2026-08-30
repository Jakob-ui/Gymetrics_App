package com.laschober.gymetrics.ui.serverconnection

sealed interface ServerConnectionState {
    data object Idle : ServerConnectionState
    data object Loading : ServerConnectionState
    data class Success(val url: String, val message: String) : ServerConnectionState
    data class Error(val message: String) : ServerConnectionState
}