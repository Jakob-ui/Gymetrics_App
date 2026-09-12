package com.laschober.gymetrics.ui.main.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.repositories.HomeRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch

sealed interface NextTrainingState {
    data object Loading : NextTrainingState
    data class Success(val training: TrainingOverviewResponseDto?) : NextTrainingState
    data class Error(val message: String) : NextTrainingState
}

class HomeScreenViewModel(
    private val homeRepository: HomeRepository,
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    var greetingName: String by mutableStateOf("")
        private set

    var nextTraining: NextTrainingState by mutableStateOf(NextTrainingState.Loading)
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
            } catch (e: Exception) {
                println("home: loading profile failed: $e")
            }
        }
    }

    fun loadNextTraining() {
        reloading = true
        viewModelScope.launch {
            if (nextTraining !is NextTrainingState.Success) {
                try {
                    trainingRepository.getCachedNextTraining()?.let {
                        nextTraining = NextTrainingState.Success(it.training)
                    }
                } catch (e: Exception) {
                    println("home: reading cached next training failed: $e")
                }
            }

            val hadContent = nextTraining is NextTrainingState.Success
            try {
                val result = trainingRepository.getNextTraining()
                nextTraining = NextTrainingState.Success(result)
            } catch (e: Exception) {
                println("home: loading next training failed: $e")
                if (hadContent) transientError = "Couldn't refresh your next training"
                else nextTraining = NextTrainingState.Error("Couldn't load your next training")
            } finally {
                reloading = false
            }
        }
    }
}
