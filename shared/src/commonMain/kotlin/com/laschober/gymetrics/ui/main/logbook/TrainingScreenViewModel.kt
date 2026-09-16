package com.laschober.gymetrics.ui.main.logbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.repositories.NoCachedDataException
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch
enum class TrainingSortOption(val label: String, val asc: Boolean) {
    NEWEST("Newest first", asc = false),
    OLDEST("Oldest first", asc = true),
}

class TrainingScreenViewModel(private val repository: TrainingRepository) : ViewModel() {

    var state: TrainingState by mutableStateOf(TrainingState.Loading)
        private set
    var loadingMore: Boolean by mutableStateOf(false)
        private set
    var endReached: Boolean by mutableStateOf(false)
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var reloading: Boolean by mutableStateOf(false)
        private set
    var transientError: String? by mutableStateOf(null)
        private set
    fun consumeTransientError() { transientError = null }
    var sortOption: TrainingSortOption by mutableStateOf(TrainingSortOption.NEWEST)
        private set
    var activeOnly: Boolean by mutableStateOf(false)
        private set

    private var page = 0
    private val pageSize = 10

    init {
        load()
    }

    fun selectSort(option: TrainingSortOption) {
        if (option == sortOption) return
        sortOption = option
        load()
    }

    fun toggleActiveOnly() {
        activeOnly = !activeOnly
        load()
    }

    private val activeFilter: Boolean? get() = if (activeOnly) true else null

    fun load() {
        page = 0
        endReached = false
        val hadContent = state is TrainingState.Success
        if (!hadContent) state = TrainingState.Loading
        reloading = true
        viewModelScope.launch {
            try {
                val list = repository.firstPage(pageSize, asc = sortOption.asc, active = activeFilter)
                page = 1
                endReached = list.size < pageSize
                state = TrainingState.Success(list)
                viewModelScope.launch { repository.prefetchTrainingDetails(list.map { it.id }) }
            } catch (e: NoCachedDataException) {
                if (hadContent) transientError = "You're offline - showing older data"
                else state = TrainingState.Error("No data available - check your connection")
            } catch (e: Exception) {
                if (hadContent) transientError = "Couldn't update the list"
                else state = TrainingState.Error("Couldn't load trainings")
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
                val list = repository.refreshFirstPage(pageSize, asc = sortOption.asc, active = activeFilter)
                page = 1
                endReached = list.size < pageSize
                state = TrainingState.Success(list)
            } catch (e: Exception) {} finally {
                refreshing = false
            }
        }
    }

    fun loadNextPage() {
        val current = state
        if (loadingMore || endReached || current !is TrainingState.Success) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val next = repository.fetchPage(page + 1, pageSize, asc = sortOption.asc, active = activeFilter)
                page += 1
                if (next.size < pageSize) endReached = true
                state = TrainingState.Success(current.trainings + next)
            } catch (e: Exception) {} finally {
                loadingMore = false
            }
        }
    }
}
