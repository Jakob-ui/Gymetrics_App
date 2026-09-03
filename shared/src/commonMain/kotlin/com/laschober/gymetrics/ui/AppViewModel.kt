package com.laschober.gymetrics.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.auth.SessionManager
import com.laschober.gymetrics.data.auth.SessionState
import kotlinx.coroutines.launch

class AppViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    var sessionState: SessionState by mutableStateOf(SessionState.Checking)
        private set

    init {
        viewModelScope.launch {
            sessionState = sessionManager.resolveSession()
        }
    }
}
