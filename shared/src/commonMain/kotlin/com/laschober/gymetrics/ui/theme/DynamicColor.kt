package com.laschober.gymetrics.ui.theme

import androidx.compose.material3.ColorScheme

// Material You / "Device colors": derived from the user's wallpaper. Android-only concept (API
// 31+), so this returns null on unsupported platforms/versions - callers fall back to the static
// DarkColorScheme/LightColorScheme in that case. See Connectivity.kt for the same expect/actual
// shape used elsewhere in this project.
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
