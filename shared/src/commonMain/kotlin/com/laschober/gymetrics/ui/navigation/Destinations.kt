package com.laschober.gymetrics.ui.navigation

import kotlinx.serialization.Serializable

class Destinations {
    // --- Outer graph (AppNavHost): whole-screen states ---
    @Serializable data object ServerConnectionRoute
    @Serializable data object LoginRoute
    @Serializable data object RegisterRoute
    @Serializable data object MainRoute

    // --- Inner graph (MainScaffold): tab content ---
    @Serializable data object HomeRoute
    @Serializable data object LogbookRoute
    @Serializable data object PlanningRoute
    @Serializable data object TemplateRoute
}
