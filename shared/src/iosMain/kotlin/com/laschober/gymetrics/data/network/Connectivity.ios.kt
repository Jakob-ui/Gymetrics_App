package com.laschober.gymetrics.data.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue


@OptIn(ExperimentalForeignApi::class)
actual fun observeConnectivity(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_set_update_handler(monitor) { path ->
        val isOnline = path != null && nw_path_get_status(path) == nw_path_status_satisfied
        trySend(isOnline)
    }
    nw_path_monitor_start(monitor)

    awaitClose { nw_path_monitor_cancel(monitor) }
}