package com.laschober.gymetrics.di

import com.laschober.gymetrics.core.network.buildHttpClient
import com.laschober.gymetrics.data.repositories.HomeRepository
import com.laschober.gymetrics.data.repositories.PendingActionQueueRepository
import com.laschober.gymetrics.data.repositories.TemplateRepository
import com.laschober.gymetrics.data.repositories.TrainingDraftRepository
import com.laschober.gymetrics.data.repositories.StudioRepository
import com.laschober.gymetrics.data.repositories.ThemeModeRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import com.laschober.gymetrics.data.sync.SyncManager
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
import com.laschober.gymetrics.ui.main.training.TrainingDetailScreenViewModel
import com.laschober.gymetrics.ui.main.training.TrainingExecutionScreenViewModel
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.laschober.gymetrics.data.local.platformFilesDir
import com.laschober.gymetrics.data.network.ConnectivityObserver
import com.laschober.gymetrics.data.local.NextTrainingCache
import com.laschober.gymetrics.data.local.PendingAction
import com.laschober.gymetrics.data.local.TrainingDraft
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TemplateResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import com.laschober.gymetrics.ui.main.templates.detail.TemplateFormScreenViewModel
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

val appModule = module {

    single { SettingStore() }
    single { ThemeModeRepository(get()) }
    single { StudioRepository(get()) }
    single { ConnectivityObserver() }
    single { TokenStore() }
    single { buildHttpClient(get(), get()) }
    single { SessionManager(get(), get(), get(), get(), get(), get(), get(), get()) }
    single(named("templatesStore")) {
        storeOf<List<TemplateOverviewResponseDto>>(
            file = Path("${platformFilesDir()}/templates.json"),
            default = emptyList(),
        )
    }
    single(named("templateDetailsStore")) {
        storeOf<Map<String, TemplateResponseDto>>(
            file = Path("${platformFilesDir()}/template_details.json"),
            default = emptyMap(),
        )
    }
    single {
        TemplateRepository(
            get(),
            get(named("templatesStore")),
            get(named("templateDetailsStore")),
            get(),
        )
    }
    single(named("trainingsStore")) {
        storeOf<List<TrainingOverviewResponseDto>>(
            file = Path("${platformFilesDir()}/trainings.json"),
            default = emptyList(),
        )
    }
    single(named("monthlyTrainingsStore")) {
        storeOf<Map<String, List<TrainingOverviewResponseDto>>>(
            file = Path("${platformFilesDir()}/trainings_by_month.json"),
            default = emptyMap(),
        )
    }
    single(named("trainingDetailsStore")) {
        storeOf<Map<String, TrainingResponseDto>>(
            file = Path("${platformFilesDir()}/training_details.json"),
            default = emptyMap(),
        )
    }
    single(named("nextTrainingCacheStore")) {
        storeOf<List<NextTrainingCache>>(
            file = Path("${platformFilesDir()}/next_training.json"),
            default = emptyList(),
        )
    }
    single(named("pendingActionsStore")) {
        storeOf<List<PendingAction>>(
            file = Path("${platformFilesDir()}/pending_actions.json"),
            default = emptyList(),
        )
    }
    single { PendingActionQueueRepository(get(named("pendingActionsStore"))) }
    single {
        TrainingRepository(
            get(),
            get(named("trainingsStore")),
            get(named("monthlyTrainingsStore")),
            get(named("trainingDetailsStore")),
            get(named("nextTrainingCacheStore")),
            get(),
            get(),
        )
    }
    single { SyncManager(get(), get(), get()) }
    single(named("trainingDraftsStore")) {
        storeOf<Map<String, TrainingDraft>>(
            file = Path("${platformFilesDir()}/training_drafts.json"),
            default = emptyMap(),
        )
    }
    single { TrainingDraftRepository(get(named("trainingDraftsStore"))) }
    single(named("profileStore")) {
        storeOf<List<UserProfileDto>>(
            file = Path("${platformFilesDir()}/profile.json"),
            default = emptyList(),
        )
    }
    single { HomeRepository(get(), get(named("profileStore"))) }

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
    viewModelOf(::TrainingExecutionScreenViewModel)
    viewModelOf(::TrainingDetailScreenViewModel)
}
