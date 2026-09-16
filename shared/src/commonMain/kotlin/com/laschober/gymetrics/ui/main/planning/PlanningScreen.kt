package com.laschober.gymetrics.ui.main.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.ui.components.PullToRefreshBoxCompat
import compose.icons.FeatherIcons
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.X
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

private const val PAGER_HALF_RANGE = 2000
private const val PAGER_CENTER = PAGER_HALF_RANGE
private const val PAGER_PAGE_COUNT = PAGER_HALF_RANGE * 2 + 1

@Composable
fun PlanningScreen(
    viewModel: PlanningScreenViewModel = koinViewModel(),
    bottomPadding: Dp = 120.dp,
    onShowMessage: (String) -> Unit = {},
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateTrainings by remember { mutableStateOf<List<TrainingOverviewResponseDto>>(emptyList()) }

    val pagerState = rememberPagerState(initialPage = PAGER_CENTER) { PAGER_PAGE_COUNT }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onWeekOffsetChanged(pagerState.currentPage - PAGER_CENTER)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WeekHeader(
            weekStart = viewModel.weekStart,
            isCurrentWeek = viewModel.weekOffset == 0,
            onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onToday = { scope.launch { pagerState.animateScrollToPage(PAGER_CENTER) } },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            WeekPage(
                weekOffset = page - PAGER_CENTER,
                viewModel = viewModel,
                bottomPadding = bottomPadding,
                onShowMessage = onShowMessage,
                onDayClick = { date, trainings ->
                    selectedDate = date
                    selectedDateTrainings = trainings
                },
            )
        }
    }

    val openDate = selectedDate
    if (openDate != null) {
        DayDetailDialog(
            date = openDate,
            trainings = selectedDateTrainings,
            viewModel = viewModel,
            onClose = {
                selectedDate = null
                viewModel.resetDialogState()
            },
        )
    }
}

@Composable
private fun WeekPage(
    weekOffset: Int,
    viewModel: PlanningScreenViewModel,
    bottomPadding: Dp,
    onShowMessage: (String) -> Unit,
    onDayClick: (LocalDate, List<TrainingOverviewResponseDto>) -> Unit,
) {
    var state by remember(weekOffset) { mutableStateOf<PlanningState>(PlanningState.Loading) }
    var reloading by remember(weekOffset) { mutableStateOf(false) }
    var refreshing by remember(weekOffset) { mutableStateOf(false) }
    var retryTick by remember(weekOffset) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(weekOffset, viewModel.refreshTrigger, retryTick) {
        val hadContent = state is PlanningState.Success
        reloading = true
        val result = viewModel.loadWeek(weekOffset)
        if (result is PlanningState.Error && hadContent) {
            onShowMessage(result.message)
        } else {
            state = result
        }
        reloading = false
    }

    val weekDates = remember(weekOffset) { viewModel.weekDatesFor(weekOffset) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (reloading && state is PlanningState.Success) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        PullToRefreshBoxCompat(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    try {
                        when (val result = viewModel.refreshWeek(weekOffset)) {
                            is PlanningState.Success -> state = result
                            is PlanningState.Error -> onShowMessage(result.message)
                            PlanningState.Loading -> Unit
                        }
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            when (val s = state) {
                PlanningState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                is PlanningState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { retryTick++ }) { Text("Try again") }
                }

                is PlanningState.Success -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (reloading) 0.55f else 1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(weekDates, key = { it.toString() }) { date ->
                        val trainings = s.trainingsByDate[date].orEmpty()
                        DayRow(
                            date = date,
                            trainings = trainings,
                            isToday = date == viewModel.today,
                            canSchedule = viewModel.canScheduleTraining(date),
                            onClick = { onDayClick(date, trainings) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(FeatherIcons.ChevronLeft, contentDescription = "Previous week")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatWeekRange(weekStart), style = MaterialTheme.typography.titleMedium)
            if (!isCurrentWeek) {
                TextButton(onClick = onToday) { Text("To today") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(FeatherIcons.ChevronRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun DayRow(
    date: LocalDate,
    trainings: List<TrainingOverviewResponseDto>,
    isToday: Boolean,
    canSchedule: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBadge(date = date, isToday = isToday)

            Spacer(Modifier.width(16.dp))

            if (trainings.isEmpty()) {
                if (canSchedule) {
                    Text(
                        text = "Plan a training",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                } else {
                    Text(
                        text = "No training",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    trainings.forEach { training -> TrainingRow(training) }
                }
            }
        }
    }
}

@Composable
private fun DateBadge(date: LocalDate, isToday: Boolean) {
    val containerColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val onColor = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = weekdayLabel(date.dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = onColor,
        )
        Text(
            text = date.day.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onColor,
        )
    }
}

@Composable
private fun TrainingRow(training: TrainingOverviewResponseDto) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = training.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (!training.status) {
            Icon(
                imageVector = FeatherIcons.CheckCircle,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DayDetailDialog(
    date: LocalDate,
    trainings: List<TrainingOverviewResponseDto>,
    viewModel: PlanningScreenViewModel,
    onClose: () -> Unit,
) {
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TrainingOverviewResponseDto?>(null) }

    Dialog(onDismissRequest = onClose) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weekdayFullLabel(date.dayOfWeek),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = formatFullDate(date), style = MaterialTheme.typography.headlineSmall)
                    }
                    IconButton(onClick = onClose) {
                        Icon(FeatherIcons.X, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                when {
                    trainings.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        trainings.forEach { training ->
                            PlannedTrainingCard(training, onDelete = { pendingDelete = training })
                        }
                    }

                    showTemplatePicker -> TemplatePicker(
                        templates = viewModel.templates,
                        loading = viewModel.loadingTemplates,
                        creating = viewModel.creatingTraining,
                        onPick = { templateId ->
                            viewModel.createTraining(date, templateId) { onClose() }
                        },
                    )

                    else -> {
                        Text(
                            text = "No training planned",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (viewModel.canScheduleTraining(date)) {
                            Button(
                                onClick = {
                                    showTemplatePicker = true
                                    viewModel.loadTemplatesIfNeeded()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(FeatherIcons.Plus, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add training")
                            }
                        } else {
                            Text(
                                text = "Trainings can only be scheduled for a future day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                viewModel.dialogError?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    pendingDelete?.let { training ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete training?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.deletingTraining,
                    onClick = {
                        viewModel.deleteTraining(training.id) {
                            pendingDelete = null
                            onClose()
                        }
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !viewModel.deletingTraining,
                    onClick = { pendingDelete = null },
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlannedTrainingCard(training: TrainingOverviewResponseDto, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = training.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!training.status) {
                    Icon(
                        imageVector = FeatherIcons.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = FeatherIcons.Trash2,
                        contentDescription = "Delete training",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (training.description.isNotBlank()) {
                Text(
                    text = training.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TemplatePicker(
    templates: List<TemplateOverviewResponseDto>?,
    loading: Boolean,
    creating: Boolean,
    onPick: (templateId: String) -> Unit,
) {
    when {
        loading || templates == null -> Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        templates.isEmpty() -> Text(
            text = "No templates yet - create one first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> LazyColumn(
            modifier = Modifier.heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(templates, key = { it.id }) { template ->
                Card(
                    onClick = { onPick(template.id) },
                    enabled = !creating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (creating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

private fun weekdayFullLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun two(n: Int) = n.toString().padStart(2, '0')

private fun formatFullDate(date: LocalDate): String =
    "${two(date.day)}.${two(date.month.ordinal + 1)}.${date.year}"

private fun formatWeekRange(start: LocalDate): String {
    val end = start.plus(6, DateTimeUnit.DAY)
    return if (start.month == end.month) {
        "${two(start.day)}. – ${two(end.day)}.${two(end.month.ordinal + 1)}.${end.year}"
    } else {
        "${two(start.day)}.${two(start.month.ordinal + 1)}. – ${two(end.day)}.${two(end.month.ordinal + 1)}.${end.year}"
    }
}
