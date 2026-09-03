package com.laschober.gymetrics.ui.main.profile

import androidx.lifecycle.ViewModel
import com.laschober.gymetrics.data.auth.SessionManager

class ProfileScreenViewModel ( private val sessionManager: SessionManager) : ViewModel() {
    fun logout() = sessionManager.logout()
}