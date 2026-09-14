package com.laschober.gymetrics.ui.main.planning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laschober.gymetrics.core.util.parseLocalDate
import com.laschober.gymetrics.core.util.todayLocalDate
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.data.repositories.TemplateRepository
import com.laschober.gymetrics.data.repositories.TrainingRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class PlanningScreenViewModel(
    private val repository: TrainingRepository,
    private val templateRepository: TemplateRepository,
) : ViewModel() {
    
    var weekOffset: Int by mutableStateOf(0)
        private set

    val today: LocalDate = todayLocalDate()

    var templates: List<TemplateOverviewResponseDto>? by mutableStateOf(null)
        private set
    var loadingTemplates: Boolean by mutableStateOf(false)
        private set
    var creatingTraining: Boolean by mutableStateOf(false)
        private set
    var deletingTraining: Boolean by mutableStateOf(false)
        private set
    var dialogError: String? by mutableStateOf(null)
        private set

    var refreshTrigger: Int by mutableStateOf(0)
        private set

    val weekStart: LocalDate
        get() = weekDatesFor(weekOffset).first()

    fun onWeekOffsetChanged(offset: Int) {
        weekOffset = offset
    }

    fun weekDatesFor(offset: Int): List<LocalDate> {
        val start = mondayOf(today).plus(offset * 7, DateTimeUnit.DAY)
        return (0..6).map { start.plus(it, DateTimeUnit.DAY) }
    }

    suspend fun loadWeek(offset: Int): PlanningState {
        val dates = weekDatesFor(offset)
        return try {
            val months = dates.map { it.year to (it.month.ordinal + 1) }.distinct()
            val trainings = months.flatMap { (year, month) -> repository.getTrainingsForMonth(year, month) }
            val byDate = trainings
                .mapNotNull { training -> parseLocalDate(training.activeDate)?.let { date -> date to training } }
                .filter { (date, _) -> date in dates }
                .groupBy({ it.first }, { it.second })
            PlanningState.Success(byDate)
        } catch (e: Exception) {
            println("planning loadWeek($offset) failed: $e")
            PlanningState.Error("Couldn't load trainings")
        }
    }

    suspend fun refreshWeek(offset: Int): PlanningState {
        val months = weekDatesFor(offset).map { it.year to (it.month.ordinal + 1) }.distinct()
        months.forEach { (year, month) -> repository.invalidateMonthCache(year, month) }
        return loadWeek(offset)
    }

    private fun mondayOf(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)

    fun canScheduleTraining(date: LocalDate): Boolean = date > today

    fun loadTemplatesIfNeeded() {
        if (templates != null || loadingTemplates) return
        loadingTemplates = true
        viewModelScope.launch {
            try {
                templates = templateRepository.firstPage(50)
            } catch (e: Exception) {
                println("planning: loading templates failed: $e")
                dialogError = "Couldn't load templates"
            } finally {
                loadingTemplates = false
            }
        }
    }

    fun createTraining(date: LocalDate, templateId: String, onCreated: () -> Unit) {
        if (creatingTraining) return
        creatingTraining = true
        dialogError = null
        viewModelScope.launch {
            try {
                repository.createTraining(templateId, date.toString())
                refreshTrigger++
                onCreated()
            } catch (e: Exception) {
                println("planning: create training failed: $e")
                dialogError = "Couldn't create training"
            } finally {
                creatingTraining = false
            }
        }
    }

    fun deleteTraining(trainingId: String, onDeleted: () -> Unit) {
        if (deletingTraining) return
        deletingTraining = true
        dialogError = null
        viewModelScope.launch {
            try {
                repository.deleteTraining(trainingId)
                refreshTrigger++
                onDeleted()
            } catch (e: Exception) {
                println("planning: delete training failed: $e")
                dialogError = "Couldn't delete training"
            } finally {
                deletingTraining = false
            }
        }
    }

    fun resetDialogState() {
        templates = null
        dialogError = null
    }
}
