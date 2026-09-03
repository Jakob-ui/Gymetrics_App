package com.laschober.gymetrics.data.auth

// Where the app should land on startup, decided by SessionManager.
sealed interface SessionState {
    data object Checking : SessionState
    data object NeedsServer : SessionState
    data object NeedsLogin : SessionState
    data object Authenticated : SessionState
}
