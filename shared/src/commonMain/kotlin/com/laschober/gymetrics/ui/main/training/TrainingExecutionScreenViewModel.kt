package com.laschober.gymetrics.ui.main.training

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.TrainingDraft
import com.laschober.gymetrics.data.repositories.TrainingDraftRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch

class TrainingExecutionScreenViewModel(
    private val repository: TrainingRepository,
    private val draftRepository: TrainingDraftRepository,
) : ViewModel() {

    var state: TrainingExecutionState by mutableStateOf(TrainingExecutionState.Loading)
        private set
    var restoredDraft: TrainingDraft? by mutableStateOf(null)
        private set

    fun load(id: String) {
        state = TrainingExecutionState.Loading
        viewModelScope.launch {
            state = try {
                val training = repository.getTraining(id).firstOrNull()
                if (training != null) {
                    restoredDraft = draftRepository.getDraft(id)
                    TrainingExecutionState.Success(training)
                } else {
                    TrainingExecutionState.Error("Training not found")
                }
            } catch (e: Exception) {
                println("training execution load failed: $e")
                TrainingExecutionState.Error("Couldn't load this training")
            }
        }
    }

    fun saveDraft(draft: TrainingDraft) {
        viewModelScope.launch {
            try {
                draftRepository.saveDraft(draft)
            } catch (e: Exception) {
                println("training execution: saving draft failed: $e")
            }
        }
    }
}
