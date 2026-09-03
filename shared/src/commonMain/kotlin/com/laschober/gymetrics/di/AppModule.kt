package com.laschober.gymetrics.di

import com.laschober.gymetrics.core.network.buildHttpClient
import com.laschober.gymetrics.data.auth.SessionManager
import com.laschober.gymetrics.data.local.SettingStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.ui.AppViewModel
import com.laschober.gymetrics.ui.auth.login.LoginScreenViewModel
import com.laschober.gymetrics.ui.auth.register.RegisterScreenViewModel
import com.laschober.gymetrics.ui.main.home.HomeScreenViewModel
import com.laschober.gymetrics.ui.main.profile.ProfileScreenViewModel
import com.laschober.gymetrics.ui.main.settings.SettingScreenViewModel
import com.laschober.gymetrics.ui.main.templates.TemplateScreenViewModel
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // --- singletons: one shared instance app-wide ---
    single { SettingStore() }
    single { TokenStore() }
    single { buildHttpClient(get(), get()) }        // get() -> SettingStore, TokenStore
    single { SessionManager(get(), get(), get()) }  // get() -> HttpClient, TokenStore, SettingStore

    // --- view models: new instance per screen, constructor params autowired ---
    viewModelOf(::AppViewModel)
    viewModelOf(::ServerConnectionViewModel)
    viewModelOf(::LoginScreenViewModel)
    viewModelOf(::RegisterScreenViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::HomeScreenViewModel)
    viewModelOf(::SettingScreenViewModel)
    viewModelOf(::TemplateScreenViewModel)
}
