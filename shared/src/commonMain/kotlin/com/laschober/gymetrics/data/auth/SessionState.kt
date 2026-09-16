package com.laschober.gymetrics.data.auth

sealed interface SessionState {
    data object Checking : SessionState
    data object NeedsServer : SessionState
    data object NeedsLogin : SessionState
    data object Authenticated : SessionState
}
