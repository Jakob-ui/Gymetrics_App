package com.laschober.gymetrics.ui.auth.login

    sealed interface LoginState {
        data object Idle : LoginState
        data object Loading : LoginState
        data object Success : LoginState
        data class Error(val message: String) : LoginState
    }