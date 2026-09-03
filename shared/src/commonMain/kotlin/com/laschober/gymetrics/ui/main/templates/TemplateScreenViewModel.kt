package com.laschober.gymetrics.ui.main.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch

class TemplateScreenViewModel(private val client: HttpClient) : ViewModel() {

    var state: TemplateState by mutableStateOf(TemplateState.Loading)
        private set
    var loadingMore: Boolean by mutableStateOf(false)
        private set
    var endReached: Boolean by mutableStateOf(false)
        private set

    private var page = 0
    private val pageSize = 10

    init { load() }

    fun load() {
        page = 0
        endReached = false
        state = TemplateState.Loading
        viewModelScope.launch {
            state = try {
                val response = client.get("templates") {
                    parameter("page", 1)
                    parameter("limit", pageSize)
                }
                if (response.status.isSuccess()) {
                    val list = response.body<List<TemplateOverviewResponseDto>>()
                    page = 1
                    if (list.size < pageSize) endReached = true
                    TemplateState.Success(list)
                } else {
                    TemplateState.Error("Couldn't load templates (${response.status.value})")
                }
            } catch (e: Exception) {
                println("templates load failed: $e")
                TemplateState.Error("Network error")
            }
        }
    }

    fun loadNextPage() {
        val current = state
        if (loadingMore || endReached || current !is TemplateState.Success) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val response = client.get("templates") {
                    parameter("page", page + 1)
                    parameter("limit", pageSize)
                }
                if (response.status.isSuccess()) {
                    val next = response.body<List<TemplateOverviewResponseDto>>()
                    page += 1
                    if (next.size < pageSize) endReached = true
                    state = TemplateState.Success(current.templates + next)
                }
            } catch (e: Exception) {
                println("templates loadNextPage failed: $e")
            } finally {
                loadingMore = false
            }
        }
    }
}
