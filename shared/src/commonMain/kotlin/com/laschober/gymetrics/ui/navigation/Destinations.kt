package com.laschober.gymetrics.ui.navigation

import kotlinx.serialization.Serializable

class Destinations {
    @Serializable data object ServerConnectionRoute
    @Serializable data object LoginRoute
    @Serializable data object RegisterRoute
    @Serializable data object MainRoute

    @Serializable data object HomeRoute
    @Serializable data object LogbookRoute
    @Serializable data object PlanningRoute
    @Serializable data object TemplateRoute

    @Serializable data class TemplateDetailRoute(val id: String, val title: String)
    @Serializable data object CreateTemplateRoute
    @Serializable data class TrainingExecutionRoute(val id: String, val title: String)
    @Serializable data class TrainingDetailRoute(val id: String, val title: String)
}
