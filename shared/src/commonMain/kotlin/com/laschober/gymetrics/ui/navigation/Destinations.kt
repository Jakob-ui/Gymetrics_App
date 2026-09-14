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

    // --- Inner graph: detail/create screens reached from a tab ---
    // `title` travels with the route so the shared TopAppBar can show it immediately,
    // without waiting for TemplateFormScreen's own GET /templates/:id to finish.
    @Serializable data class TemplateDetailRoute(val id: String, val title: String)
    @Serializable data object CreateTemplateRoute
    @Serializable data class TrainingExecutionRoute(val id: String, val title: String)
    @Serializable data class TrainingDetailRoute(val id: String, val title: String)
}
