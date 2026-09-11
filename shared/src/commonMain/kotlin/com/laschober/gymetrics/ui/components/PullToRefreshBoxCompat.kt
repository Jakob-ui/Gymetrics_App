package com.laschober.gymetrics.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Material3's PullToRefreshBox exists for Android and iOS individually (Android via the real
// androidx.compose.material3 artifact, iOS via JetBrains' own implementation), but isn't exposed
// from the commonMain shared by botah in this Compose Multiplatform version - see the .android.kt /
// .ios.kt actuals, which each just forward to their platform's real PullToRefreshBox.
@Composable
expect fun PullToRefreshBoxCompat(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
)
