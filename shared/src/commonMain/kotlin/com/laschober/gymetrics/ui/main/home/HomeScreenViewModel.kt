package com.laschober.gymetrics.ui.main.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import com.laschober.gymetrics.data.repositories.TrainingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch

sealed interface NextTrainingState {
    data object Loading : NextTrainingState
    // training == null means nothing is scheduled - a valid, non-error result.
    data class Success(val training: TrainingOverviewResponseDto?) : NextTrainingState
    data class Error(val message: String) : NextTrainingState
}

class HomeScreenViewModel(
    private val client: HttpClient,
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
        // loadNextTraining() is NOT called here - HomeScreen calls it from a LaunchedEffect(Unit)
        // every time the Home tab is entered, not just once. The NavHost keeps this ViewModel
        // alive across tab switches, so without that the card would go stale after e.g.
        // scheduling/deleting a training in Planning.
    }

    // Best-effort - the greeting just falls back to no name if this fails, not worth its own
    // error state.
    private fun loadGreetingName() {
        viewModelScope.launch {
            try {
                val response = client.get("user/profile")
                if (response.status.isSuccess()) {
                    greetingName = response.body<UserProfileDto>().name
                }
            } catch (e: Exception) {
                println("home: loading profile failed: $e")
            }
        }
    }

    fun loadNextTraining() {
        val hadContent = nextTraining is NextTrainingState.Success
        if (!hadContent) nextTraining = NextTrainingState.Loading
        reloading = true
        viewModelScope.launch {
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
