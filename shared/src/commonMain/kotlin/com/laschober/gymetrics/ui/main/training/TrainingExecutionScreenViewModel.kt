package com.laschober.gymetrics.ui.main.training

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.local.TrainingDraft
import com.laschober.gymetrics.data.remote.dto.ExerciseDoneRequestDto
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
    var completing: Boolean by mutableStateOf(false)
        private set
    var completeError: String? by mutableStateOf(null)
        private set
    fun consumeCompleteError() { completeError = null }

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

    // Sets active=false and submits the entered weight/reps per exercise. If offline,
    // TrainingRepository queues it (plan included) and returns normally - so onCompleted() still
    // runs (and the draft still gets cleared) even before it's actually synced, matching how "Add
    // training"/"Delete training" already behave in Planning.
    fun completeTraining(plan: List<ExerciseDoneRequestDto>, onCompleted: () -> Unit) {
        if (completing) return
        val trainingId = (state as? TrainingExecutionState.Success)?.training?.id ?: return
        completing = true
        completeError = null
        viewModelScope.launch {
            try {
                repository.completeTraining(trainingId, plan)
                draftRepository.clearDraft(trainingId)
                onCompleted()
            } catch (e: Exception) {
                println("training execution: complete training failed: $e")
                completeError = e.message ?: "Couldn't complete training"
            } finally {
                completing = false
            }
        }
    }
}
