package com.laschober.gymetrics.ui.main.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.core.util.parseLocalDate
import com.laschober.gymetrics.core.util.todayLocalDate
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.repositories.HomeRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

sealed interface HomeTrainingsState {
    data object Loading : HomeTrainingsState
    data class Success(val today: TrainingOverviewResponseDto?, val next: TrainingOverviewResponseDto?) : HomeTrainingsState
    data class Error(val message: String) : HomeTrainingsState
}

class HomeScreenViewModel(
    private val homeRepository: HomeRepository,
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    var greetingName: String by mutableStateOf("")
        private set

    var homeTrainings: HomeTrainingsState by mutableStateOf(HomeTrainingsState.Loading)
        private set
    var reloading: Boolean by mutableStateOf(false)
        private set
    var transientError: String? by mutableStateOf(null)
        private set
    fun consumeTransientError() { transientError = null }

    init {
        loadGreetingName()
    }

    private fun loadGreetingName() {
        viewModelScope.launch {
            try {
                greetingName = homeRepository.getGreetingName()
            } catch (e: Exception) {}
        }
    }

    fun loadHomeTrainings() {
        val hadContent = homeTrainings is HomeTrainingsState.Success
        if (!hadContent) homeTrainings = HomeTrainingsState.Loading
        reloading = true
        viewModelScope.launch {
            try {
                val today = todayLocalDate()
                val monthTrainings = trainingRepository.getTrainingsForMonth(today.year, today.month.ordinal + 1)
                val dated = monthTrainings.mapNotNull { t -> parseLocalDate(t.activeDate)?.let { it to t } }

                val todaysTraining = dated.find { (date, _) -> date == today }?.second

                val upcoming = dated
                    .filter { (date, _) -> date > today }
                    .sortedBy { it.first }
                    .firstOrNull()?.second
                    ?: fetchFirstFromNextMonth(today)

                homeTrainings = HomeTrainingsState.Success(todaysTraining, upcoming)
            } catch (e: Exception) {
                if (hadContent) transientError = "Couldn't refresh your trainings"
                else homeTrainings = HomeTrainingsState.Error("Couldn't load your trainings")
            } finally {
                reloading = false
            }
        }
    }

    private suspend fun fetchFirstFromNextMonth(today: LocalDate): TrainingOverviewResponseDto? {
        val nextMonth = LocalDate(today.year, today.month, 1).plus(1, DateTimeUnit.MONTH)
        val nextMonthTrainings = trainingRepository.getTrainingsForMonth(nextMonth.year, nextMonth.month.ordinal + 1)
        return nextMonthTrainings
            .mapNotNull { t -> parseLocalDate(t.activeDate)?.let { it to t } }
            .sortedBy { it.first }
            .firstOrNull()?.second
    }
}
