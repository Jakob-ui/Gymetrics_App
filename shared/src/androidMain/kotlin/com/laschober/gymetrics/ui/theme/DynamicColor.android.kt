package com.laschober.gymetrics.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import com.laschober.gymetrics.data.local.appContext

actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = appContext ?: return null
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
