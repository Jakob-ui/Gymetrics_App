package com.laschober.gymetrics.ui.main.training

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch

sealed interface TrainingDetailState {
    data object Loading : TrainingDetailState
    data class Error(val message: String) : TrainingDetailState
    data class Success(val current: TrainingResponseDto, val previous: TrainingResponseDto?) : TrainingDetailState
}

class TrainingDetailScreenViewModel(
    private val repository: TrainingRepository,
) : ViewModel() {

    var state: TrainingDetailState by mutableStateOf(TrainingDetailState.Loading)
        private set

    fun load(id: String) {
        state = TrainingDetailState.Loading
        viewModelScope.launch {
            state = try {
                val result = repository.getTraining(id)
                val current = result.firstOrNull()
                if (current != null) {
                    TrainingDetailState.Success(current, result.getOrNull(1))
                } else {
                    TrainingDetailState.Error("Training not found")
                }
            } catch (e: Exception) {
                println("training detail load failed: $e")
                TrainingDetailState.Error("Couldn't load this training")
            }
        }
    }
}
