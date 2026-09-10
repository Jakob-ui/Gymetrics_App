package com.laschober.gymetrics.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.laschober.gymetrics.data.local.appContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun observeConnectivity(): Flow<Boolean> = callbackFlow @androidx.annotation.RequiresPermission(
    android.Manifest.permission.ACCESS_NETWORK_STATE
) {
    val context = appContext ?: error("Call initAndroidContext() in MainActivity.onCreate first")
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            trySend(hasInternet)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    connectivityManager.registerDefaultNetworkCallback(callback)
    trySend(connectivityManager.activeNetwork != null)

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}