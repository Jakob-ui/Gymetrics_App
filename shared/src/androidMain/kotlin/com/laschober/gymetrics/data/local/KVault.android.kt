package com.laschober.gymetrics.data.local

import android.content.Context
import com.liftric.kvault.KVault

    private var appContext: Context? = null

    fun initAndroidContext(context: Context) {
        appContext = context.applicationContext
    }

    actual fun createKVault(): KVault =
        KVault(appContext ?: error("Call initAndroidContext() in MainActivity.onCreate first"))