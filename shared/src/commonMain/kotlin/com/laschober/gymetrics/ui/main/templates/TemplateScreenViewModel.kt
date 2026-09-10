package com.laschober.gymetrics.ui.main.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.repositories.NoCachedDataException
import com.laschober.gymetrics.data.repositories.TemplateRepository
import com.laschober.gymetrics.data.repositories.TemplateSortBy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

// ASCENDING (sortBy/asc both null) is what firstPage()/refreshFirstPage() cache for offline use -
// it's what the backend does by default anyway (createdAt ascending), so leaving both null here
// keeps that path untouched and cache-eligible; DESCENDING is explicit and always network-only.
enum class TemplateSortOption(val label: String, val sortBy: TemplateSortBy?, val asc: Boolean?) {
    ASCENDING("Created ↑", null, null),
    DESCENDING("Created ↓", TemplateSortBy.CREATED_AT, false),
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
    var sortOption: TemplateSortOption by mutableStateOf(TemplateSortOption.ASCENDING)
        private set

    // Backs the debounce below - `query` itself updates the text field instantly,
    // this is only used to delay when the actual network request fires.
    private val queryFlow = MutableStateFlow("")

    private var page = 0
    private val pageSize = 10

    init {
        load()
        viewModelScope.launch {
            // drop(1): skip the flow's initial "" value, load() above already handles the first fetch.
            queryFlow
                .drop(1)
                .debounce(300)
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    // Called on every keystroke in the search field.
    fun updateQuery(value: String) {
        query = value
        queryFlow.value = value
    }

    // Reloads immediately - no debounce needed, this is a deliberate tap, not something that
    // fires repeatedly like typing.
    fun selectSort(option: TemplateSortOption) {
        if (option == sortOption) return
        sortOption = option
        load()
    }

    fun load() {
        page = 0
        endReached = false
        state = TemplateState.Loading
        viewModelScope.launch {
            state = try {
                val list = repository.firstPage(
                    pageSize,
                    search = query,
                    sortBy = sortOption.sortBy,
                    asc = sortOption.asc,
                )
                page = 1
                endReached = list.size < pageSize
                TemplateState.Success(list)
            } catch (e: NoCachedDataException) {
                TemplateState.Error("No data available - check your connection")
            } catch (e: Exception) {
                println("templates load failed: $e")
                TemplateState.Error("Couldn't search templates")
            }
        }
    }

    // Pull-to-refresh: keeps whatever is currently shown if the refresh itself fails,
    // instead of replacing the list with an error state.
    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try {
                val list = repository.refreshFirstPage(
                    pageSize,
                    search = query,
                    sortBy = sortOption.sortBy,
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
                    sortBy = sortOption.sortBy,
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
