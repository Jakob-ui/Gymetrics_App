package com.laschober.gymetrics.ui.main.templates.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.data.remote.dto.TemplateExerciseDto
import com.laschober.gymetrics.data.remote.dto.TemplateRequestDto
import com.laschober.gymetrics.data.repositories.TemplateRepository
import kotlinx.coroutines.launch

class TemplateFormScreenViewModel(
    private val repository: TemplateRepository,
) : ViewModel() {

    var state: TemplateFormState by mutableStateOf(TemplateFormState.Loading)
        private set
    private var nextLocalId = 0
    private fun newLocalId() = "local-${nextLocalId++}"

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
                            weight = it.weight.toString(),
                            factor = it.factor,
                        )
                    },
                )
            } catch (e: Exception) {
                println("template form load failed: $e")
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
        it.copy(exercises = it.exercises + ExerciseFormItem(newLocalId(), title = "", reps = "", weight = "", factor = null))
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

    fun updateExerciseWeight(localId: String, value: String) = update { editing ->
        editing.copy(exercises = editing.exercises.map { if (it.localId == localId) it.copy(weight = value) else it })
    }

    // Called from the reorderable list's onMove(from, to).
    fun moveExercise(from: Int, to: Int) = update { editing ->
        editing.copy(exercises = editing.exercises.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun save(onSaved: () -> Unit) {
        val current = state
        if (current !is TemplateFormState.Editing || current.title.isBlank()) return
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
                println("template save failed: $e")
                update { it.copy(saving = false, error = "Couldn't save template") }
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
                println("template delete failed: $e")
                update { it.copy(saving = false, error = "Couldn't delete template") }
            }
        }
    }
}
