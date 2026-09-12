package com.laschober.gymetrics.ui.main.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.repositories.NoCachedDataException
import com.laschober.gymetrics.data.repositories.TemplateRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

enum class TemplateSortOption(val label: String, val asc: Boolean) {
    NEWEST("Newest first", asc = false),
    OLDEST("Oldest first", asc = true),
}

@OptIn(FlowPreview::class)
class TemplateScreenViewModel(private val repository: TemplateRepository) : ViewModel() {

    var state: TemplateState by mutableStateOf(TemplateState.Loading)
        private set
    var loadingMore: Boolean by mutableStateOf(false)
        private set
    var endReached: Boolean by mutableStateOf(false)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var query: String by mutableStateOf("")
        private set
    var sortOption: TemplateSortOption by mutableStateOf(TemplateSortOption.NEWEST)
        private set
    var reloading: Boolean by mutableStateOf(false)
        private set
    var transientError: String? by mutableStateOf(null)
        private set
    fun consumeTransientError() { transientError = null }

    private val queryFlow = MutableStateFlow("")

    private var page = 0
    private val pageSize = 10

    init {
        load()
        viewModelScope.launch {
            queryFlow
                .drop(1)
                .debounce(300)
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    fun updateQuery(value: String) {
        query = value
        queryFlow.value = value
    }

    fun selectSort(option: TemplateSortOption) {
        if (option == sortOption) return
        sortOption = option
        load()
    }

    fun load() {
        page = 0
        endReached = false
        val hadContent = state is TemplateState.Success
        if (!hadContent) state = TemplateState.Loading
        reloading = true
        viewModelScope.launch {
            try {
                val list = repository.firstPage(pageSize, search = query, asc = sortOption.asc)
                page = 1
                endReached = list.size < pageSize
                state = TemplateState.Success(list)
                // Fire-and-forget: warms the detail cache for these ids in the background so
                // opening one is instant later. Doesn't block the list from showing.
                viewModelScope.launch { repository.prefetchTemplateDetails(list.map { it.id }) }
            } catch (e: NoCachedDataException) {
                if (hadContent) transientError = "You're offline - showing older data"
                else state = TemplateState.Error("No data available - check your connection")
            } catch (e: Exception) {
                println("templates load failed: $e")
                if (hadContent) transientError = "Couldn't update the list"
                else state = TemplateState.Error("Couldn't load templates")
            } finally {
                reloading = false
            }
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try {
                val list = repository.refreshFirstPage(
                    pageSize,
                    search = query,
                    asc = sortOption.asc,
                )
                page = 1
                endReached = list.size < pageSize
                state = TemplateState.Success(list)
            } catch (e: Exception) {
                println("templates refresh failed: $e")
            } finally {
                refreshing = false
            }
        }
    }

    fun loadNextPage() {
        val current = state
        if (loadingMore || endReached || current !is TemplateState.Success) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val next = repository.fetchPage(
                    page + 1,
                    pageSize,
                    search = query,
                    asc = sortOption.asc,
                )
                page += 1
                if (next.size < pageSize) endReached = true
                state = TemplateState.Success(current.templates + next)
            } catch (e: Exception) {
                println("templates loadNextPage failed: $e")
            } finally {
                loadingMore = false
            }
        }
    }
}
