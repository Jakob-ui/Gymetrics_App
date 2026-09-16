package com.laschober.gymetrics.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetStatusBarAppearance(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context as? Activity ?: return
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !isDarkTheme
    }
}
