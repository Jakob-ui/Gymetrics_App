package com.laschober.gymetrics.di

import com.laschober.gymetrics.core.network.buildHttpClient
import com.laschober.gymetrics.data.repositories.TemplateRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import com.laschober.gymetrics.data.auth.SessionManager
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.ui.AppViewModel
import com.laschober.gymetrics.ui.auth.login.LoginScreenViewModel
import com.laschober.gymetrics.ui.auth.register.RegisterScreenViewModel
import com.laschober.gymetrics.ui.main.home.HomeScreenViewModel
import com.laschober.gymetrics.ui.main.logbook.TrainingScreenViewModel
import com.laschober.gymetrics.ui.main.planning.PlanningScreenViewModel
import com.laschober.gymetrics.ui.main.profile.ProfileScreenViewModel
import com.laschober.gymetrics.ui.main.settings.SettingScreenViewModel
import com.laschober.gymetrics.ui.main.templates.TemplateScreenViewModel
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.laschober.gymetrics.data.local.platformFilesDir
import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.ui.main.templates.detail.TemplateFormScreenViewModel
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

val appModule = module {

    single { SettingStore() }
    single { ConnectivityObserver() }
    single { TokenStore() }
    single { buildHttpClient(get(), get()) }
    single { SessionManager(get(), get(), get(), get()) }
    // Named qualifiers: Kotlin's generic type parameters are erased at runtime, so without a
    // name, Koin can't tell KStore<List<TemplateOverviewResponseDto>> and
    // KStore<List<TrainingOverviewResponseDto>> apart - both just look like "a KStore" to it,
    // and one repository silently gets the other's store.
    single(named("templatesStore")) {
        storeOf<List<TemplateOverviewResponseDto>>(
            file = Path("${platformFilesDir()}/templates.json"),
            default = emptyList(),
        )
    }
    single { TemplateRepository(get(), get(named("templatesStore")), get()) }
    single(named("trainingsStore")) {
        storeOf<List<TrainingOverviewResponseDto>>(
            file = Path("${platformFilesDir()}/trainings.json"),
            default = emptyList(),
        )
    }
    single { TrainingRepository(get(), get(named("trainingsStore")), get()) }

    viewModelOf(::AppViewModel)
    viewModelOf(::ServerConnectionViewModel)
    viewModelOf(::LoginScreenViewModel)
    viewModelOf(::RegisterScreenViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::HomeScreenViewModel)
    viewModelOf(::SettingScreenViewModel)
    viewModelOf(::TemplateScreenViewModel)
    viewModelOf(::TemplateFormScreenViewModel)
    viewModelOf(::TrainingScreenViewModel)
    viewModelOf(::PlanningScreenViewModel)
}
