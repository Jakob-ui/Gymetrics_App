package com.laschober.gymetrics.data.local

// Path to a directory the app may write to. Each platform resolves this differently
// (Android needs a Context, iOS asks NSFileManager) - see the .android.kt / .ios.kt actuals.
expect fun platformFilesDir(): String
