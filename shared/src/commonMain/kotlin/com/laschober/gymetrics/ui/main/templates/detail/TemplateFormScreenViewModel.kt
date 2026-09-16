package com.laschober.gymetrics.ui.main.templates.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TemplateExerciseDto
import com.laschober.gymetrics.data.remote.dto.TemplateRequestDto
import com.laschober.gymetrics.data.remote.dto.UserProfileDto
import com.laschober.gymetrics.data.repositories.TemplateRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch

class TemplateFormScreenViewModel(
    private val repository: TemplateRepository,
    private val client: HttpClient,
) : ViewModel() {

    var state: TemplateFormState by mutableStateOf(TemplateFormState.Loading)
        private set
    private var nextLocalId = 0
    private fun newLocalId() = "local-${nextLocalId++}"

    var aiDialogVisible: Boolean by mutableStateOf(false)
        private set
    var aiMessage: String by mutableStateOf("")
        private set
    var aiSubmitting: Boolean by mutableStateOf(false)
        private set
    var aiError: String? by mutableStateOf(null)
        private set
    var activeStudio: String? by mutableStateOf(null)
        private set

    fun openAiDialog() {
        aiDialogVisible = true
        aiError = null
        if (activeStudio == null) {
            viewModelScope.launch {
                try {
                    val response = client.get("user/profile")
                    if (response.status.isSuccess()) {
                        activeStudio = response.body<UserProfileDto>().activeStudio
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun dismissAiDialog() {
        aiDialogVisible = false
        aiError = null
    }

    fun updateAiMessage(value: String) {
        aiMessage = value
    }

    fun submitAiGeneration(onStarted: () -> Unit) {
        if (aiSubmitting) return
        val studio = activeStudio
        if (studio.isNullOrBlank()) {
            aiError = "Choose a studio in Settings first so the AI knows your equipment"
            return
        }
        aiSubmitting = true
        aiError = null
        viewModelScope.launch {
            try {
                repository.generateTemplateWithAi(studio, aiMessage.trim().ifBlank { null })
                aiDialogVisible = false
                onStarted()
            } catch (e: Exception) {
                aiError = e.message ?: "Couldn't start AI generation"
            } finally {
                aiSubmitting = false
            }
        }
    }

    fun load(id: String?) {
        if (id == null) {
            state = TemplateFormState.Editing(
                templateId = null,
                title = "",
                description = "",
                icon = "barbell-outline",
                exercises = emptyList(),
            )
            return
        }
        state = TemplateFormState.Loading
        viewModelScope.launch {
            state = try {
                val template = repository.getTemplate(id)
                TemplateFormState.Editing(
                    templateId = template.id,
                    title = template.title,
                    description = template.description,
                    icon = template.icon,
                    exercises = template.plan.map {
                        ExerciseFormItem(
                            localId = newLocalId(),
                            title = it.title,
                            reps = it.reps.toString(),
                            sets = it.sets.toString(),
                            weight = it.weight.toString(),
                            factor = it.factor,
                        )
                    },
                )
            } catch (e: Exception) {
                TemplateFormState.LoadError("Couldn't load template")
            }
        }
    }

    private fun update(transform: (TemplateFormState.Editing) -> TemplateFormState.Editing) {
        val current = state
        if (current is TemplateFormState.Editing) state = transform(current)
    }

    fun updateTitle(value: String) = update { it.copy(title = value) }
    fun updateDescription(value: String) = update { it.copy(description = value) }

    fun addExercise() = update {
        it.copy(
            exercises = it.exercises + ExerciseFormItem(
                newLocalId(),
                title = "",
                reps = "",
                sets = "",
                weight = "",
                factor = null,
            ),
        )
    }

    fun removeExercise(localId: String) = update { editing ->
        editing.copy(exercises = editing.exercises.filterNot { it.localId == localId })
    }

    fun updateExerciseTitle(localId: String, value: String) = update { editing ->
        editing.copy(exercises = editing.exercises.map { if (it.localId == localId) it.copy(title = value) else it })
    }

    fun updateExerciseReps(localId: String, value: String) = update { editing ->
        editing.copy(exercises = editing.exercises.map { if (it.localId == localId) it.copy(reps = value) else it })
    }

    fun updateExerciseSets(localId: String, value: String) = update { editing ->
        editing.copy(exercises = editing.exercises.map { if (it.localId == localId) it.copy(sets = value) else it })
    }

    fun updateExerciseWeight(localId: String, value: String) = update { editing ->
        editing.copy(exercises = editing.exercises.map { if (it.localId == localId) it.copy(weight = value) else it })
    }

    fun moveExercise(from: Int, to: Int) = update { editing ->
        editing.copy(exercises = editing.exercises.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun save(onSaved: () -> Unit) {
        val current = state
        if (current !is TemplateFormState.Editing) return
        if (current.title.isBlank()) {
            update { it.copy(error = "Please enter a title") }
            return
        }
        state = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                val request = TemplateRequestDto(
                    title = current.title,
                    description = current.description,
                    icon = current.icon,
                    plan = current.exercises.map {
                        TemplateExerciseDto(
                            title = it.title,
                            reps = it.reps.toIntOrNull() ?: 0,
                            sets = it.sets.toIntOrNull() ?: 0,
                            weight = it.weight.toDoubleOrNull() ?: 0.0,
                            factor = it.factor,
                        )
                    },
                )
                if (current.templateId == null) {
                    repository.createTemplate(request)
                } else {
                    repository.updateTemplate(current.templateId, request)
                }
                onSaved()
            } catch (e: Exception) {
                update { it.copy(saving = false, error = e.message ?: "Couldn't save template") }
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = state
        val id = (current as? TemplateFormState.Editing)?.templateId ?: return
        state = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.deleteTemplate(id)
                onDeleted()
            } catch (e: Exception) {
                update { it.copy(saving = false, error = e.message ?: "Couldn't delete template") }
            }
        }
    }
}
