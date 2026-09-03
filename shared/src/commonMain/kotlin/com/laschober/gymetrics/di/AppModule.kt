package com.laschober.gymetrics.di

import com.laschober.gymetrics.core.network.buildHttpClient
import com.laschober.gymetrics.data.auth.SessionManager
import com.laschober.gymetrics.data.local.ServerUrlStore
import com.laschober.gymetrics.data.local.TokenStore
import com.laschober.gymetrics.ui.AppViewModel
import com.laschober.gymetrics.ui.auth.login.LoginScreenViewModel
import com.laschober.gymetrics.ui.main.profile.ProfileScreenViewModel
import com.laschober.gymetrics.ui.auth.register.RegisterScreenViewModel
import com.laschober.gymetrics.ui.main.home.HomeScreenViewModel
import com.laschober.gymetrics.ui.serverconnection.ServerConnectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

        single { ServerUrlStore() }

        single { TokenStore() }

        single { buildHttpClient(get(), get()) }

        single { SessionManager(get(), get(), get())}

    viewModelOf(::AppViewModel)
    viewModelOf(::ServerConnectionViewModel)
    viewModelOf(::LoginScreenViewModel)
    viewModelOf(::RegisterScreenViewModel)
    viewModelOf(::ProfileScreenViewModel)
    viewModelOf(::HomeScreenViewModel)
    }