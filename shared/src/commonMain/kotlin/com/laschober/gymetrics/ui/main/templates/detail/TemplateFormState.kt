package com.laschober.gymetrics.ui.main.templates.detail

sealed interface TemplateFormState {
    data object Loading : TemplateFormState
    data class LoadError(val message: String) : TemplateFormState

    data class Editing(
        val templateId: String?,
        val title: String,
        val description: String,
        val icon: String,
        val exercises: List<ExerciseFormItem>,
        val saving: Boolean = false,
        val error: String? = null,
    ) : TemplateFormState {
        val isNew: Boolean get() = templateId == null
    }
}
data class ExerciseFormItem(
    val localId: String,
    val title: String,
    val reps: String,
    val sets: String,
    val weight: String,
    val factor: Double?,
)
