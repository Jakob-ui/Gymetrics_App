package com.laschober.gymetrics.data.auth

// Where the app should land on startup, decided by SessionManager.
sealed interface SessionState {
    data object Checking : SessionState        // check still running -> show splash
    data object NeedsServer : SessionState     // no server URL stored -> ServerConnection
    data object NeedsLogin : SessionState      // no / invalid tokens -> Login
    data object Authenticated : SessionState   // valid session -> Home
}
