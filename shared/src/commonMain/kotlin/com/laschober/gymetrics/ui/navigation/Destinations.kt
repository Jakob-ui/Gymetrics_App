package com.laschober.gymetrics.ui.navigation

import kotlinx.serialization.Serializable

class Destinations {
    @Serializable
    data object ServerConnectionRoute

    @Serializable
    data object LoginRoute

    @Serializable
    data object RegisterRoute

    @Serializable
    data object HomeRoute
}