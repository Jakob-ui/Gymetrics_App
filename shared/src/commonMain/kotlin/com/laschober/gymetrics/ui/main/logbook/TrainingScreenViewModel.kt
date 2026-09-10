package com.laschober.gymetrics.ui.main.logbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.repositories.NoCachedDataException
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch

// ASCENDING (asc = null) is what firstPage()/refreshFirstPage() cache for offline use - it's what
// the backend does by default anyway, so leaving it null keeps that path cache-eligible.
enum class TrainingSortOption(val label: String, val asc: Boolean?) {
    ASCENDING("Created ↑", null),
    DESCENDING("Created ↓", false),
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
    var sortOption: TrainingSortOption by mutableStateOf(TrainingSortOption.ASCENDING)
        private set
    var activeOnly: Boolean by mutableStateOf(false)
        private set

    private var page = 0
    private val pageSize = 10

    init {
        load()
    }

    // Reloads immediately - a deliberate tap, no debounce needed.
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
        state = TrainingState.Loading
        viewModelScope.launch {
            state = try {
                val list = repository.firstPage(pageSize, asc = sortOption.asc, active = activeFilter)
                page = 1
                endReached = list.size < pageSize
                TrainingState.Success(list)
            } catch (e: NoCachedDataException) {
                TrainingState.Error("No data available - check your connection")
            } catch (e: Exception) {
                println("trainings load failed: $e")
                TrainingState.Error("Couldn't load trainings")
            }
        }
    }

    // Pull-to-refresh: keeps whatever is currently shown if the refresh itself fails.
    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try {
                val list = repository.refreshFirstPage(pageSize, asc = sortOption.asc, active = activeFilter)
                page = 1
                endReached = list.size < pageSize
                state = TrainingState.Success(list)
            } catch (e: Exception) {
                println("trainings refresh failed: $e")
            } finally {
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
            } catch (e: Exception) {
                println("trainings loadNextPage failed: $e")
            } finally {
                loadingMore = false
            }
        }
    }
}
