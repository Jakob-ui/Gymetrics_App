package com.laschober.gymetrics.ui.main.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.core.util.formatDateTime
import com.laschober.gymetrics.data.remote.dto.TrainingExerciseDto
import com.laschober.gymetrics.data.remote.dto.TrainingResponseDto
import com.laschober.gymetrics.ui.components.Pill
import com.laschober.gymetrics.ui.theme.extendedColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Minus
import compose.icons.feathericons.TrendingDown
import compose.icons.feathericons.TrendingUp
import org.koin.compose.viewmodel.koinViewModel

private val cardShape = RoundedCornerShape(20.dp)

@Composable
fun TrainingDetailScreen(
    id: String,
    viewModel: TrainingDetailScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { viewModel.load(id) }

    when (val s = viewModel.state) {
        TrainingDetailState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is TrainingDetailState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { viewModel.load(id) }) { Text("Try again") }
        }

        is TrainingDetailState.Success -> TrainingDetailContent(current = s.current, previous = s.previous)
    }
}

@Composable
private fun TrainingDetailContent(current: TrainingResponseDto, previous: TrainingResponseDto?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatDateTime(current.activeDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (current.description.isNotBlank()) {
                    Text(
                        text = current.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (previous == null) {
            item {
                Text(
                    text = "No previous training with this template yet - nothing to compare against.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(current.plan.size) { index ->
            val exercise = current.plan[index]
            val previousExercise = previous?.plan?.getOrNull(index)
            val comparison = compareExercise(
                currentWeightDone = exercise.setsDone.maxOfOrNull { it.weight },
                currentRepsDone = exercise.setsDone.sumOf { it.reps }.takeIf { exercise.setsDone.isNotEmpty() },
                previousWeightDone = previousExercise?.setsDone?.maxOfOrNull { it.weight },
                previousRepsDone = previousExercise?.setsDone?.sumOf { it.reps }
                    ?.takeIf { previousExercise.setsDone.isNotEmpty() },
            )
            ExerciseComparisonCard(exercise = exercise, comparison = comparison, hasPrevious = previous != null)
        }
    }
}

@Composable
private fun ExerciseComparisonCard(
    exercise: TrainingExerciseDto,
    comparison: ExerciseComparison,
    hasPrevious: Boolean,
) {
    Card(
        shape = cardShape,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${exercise.sets} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasPrevious && comparison.overall != Overall.UNKNOWN) {
                    OverallBadge(comparison.overall)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (exercise.setsDone.isEmpty()) {
                Text(
                    text = "No sets recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                exercise.setsDone.forEachIndexed { setIndex, set ->
                    if (setIndex > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Set ${setIndex + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${exercise.weight ?: 0} kg × ${exercise.reps} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${set.weight} kg × ${set.reps} reps",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ComparisonRow(
                    label = "Best set",
                    target = "${exercise.weight ?: 0} kg",
                    done = exercise.setsDone.maxOfOrNull { it.weight }?.let { "$it kg" } ?: "-",
                    trend = comparison.weightTrend,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ComparisonRow(
                    label = "Total reps",
                    target = "${exercise.reps * exercise.sets} reps",
                    done = exercise.setsDone.sumOf { it.reps }.toString(),
                    trend = comparison.repsTrend,
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(label: String, target: String, done: String, trend: Trend) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = target,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(done, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            trendIcon(trend)?.let { (icon, color) ->
                Spacer(Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OverallBadge(overall: Overall) {
    val (label, container, content) = when (overall) {
        Overall.PROGRESS -> Triple("Progress", MaterialTheme.extendedColors.success, MaterialTheme.extendedColors.onSuccess)
        Overall.DECLINE -> Triple("Decline", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        Overall.SAME -> Triple("Same", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        Overall.MIXED -> Triple("Mixed", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        Overall.UNKNOWN -> return
    }
    Pill(text = label, containerColor = container, contentColor = content)
}

@Composable
private fun trendIcon(trend: Trend): Pair<ImageVector, androidx.compose.ui.graphics.Color>? = when (trend) {
    Trend.UP -> FeatherIcons.TrendingUp to MaterialTheme.extendedColors.success
    Trend.DOWN -> FeatherIcons.TrendingDown to MaterialTheme.colorScheme.error
    Trend.SAME -> FeatherIcons.Minus to MaterialTheme.colorScheme.onSurfaceVariant
    Trend.UNKNOWN -> null
}
