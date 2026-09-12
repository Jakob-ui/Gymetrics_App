package com.laschober.gymetrics.ui.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.core.util.formatDateTime
import com.laschober.gymetrics.core.util.parseLocalDate
import com.laschober.gymetrics.core.util.todayLocalDate
import com.laschober.gymetrics.data.remote.dto.TrainingOverviewResponseDto
import com.laschober.gymetrics.ui.components.Pill
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Play
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel(),
    onStartTraining: (trainingId: String, title: String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.loadNextTraining() }

    LaunchedEffect(viewModel.transientError) {
        viewModel.transientError?.let {
            onShowMessage(it)
            viewModel.consumeTransientError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = if (viewModel.greetingName.isNotBlank()) "Hello, ${viewModel.greetingName}" else "Hello",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Ready for your training?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Next workout",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (viewModel.reloading && viewModel.nextTraining is NextTrainingState.Success) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(4.dp))

        when (val s = viewModel.nextTraining) {
            NextTrainingState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is NextTrainingState.Error -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { viewModel.loadNextTraining() }) { Text("Try again") }
                }
            }

            is NextTrainingState.Success -> {
                val training = s.training
                if (training == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().alpha(if (viewModel.reloading) 0.55f else 1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Nothing scheduled", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Plan your next training in the Planning tab",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    NextTrainingCard(
                        training = training,
                        onStartTraining = { onStartTraining(training.id, training.title) },
                        modifier = Modifier.alpha(if (viewModel.reloading) 0.55f else 1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextTrainingCard(
    training: TrainingOverviewResponseDto,
    onStartTraining: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    text = formatDateTime(training.activeDate),
                    icon = FeatherIcons.Clock,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                relativeDayLabel(training.activeDate)?.let { label ->
                    Pill(
                        text = label,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = training.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (training.description.isNotBlank()) {
                    Text(
                        text = training.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Start training", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(FeatherIcons.Play, contentDescription = null)
            }
        }
    }
}

private fun relativeDayLabel(iso: String): String? {
    val date = parseLocalDate(iso) ?: return null
    return when (date.toEpochDays() - todayLocalDate().toEpochDays()) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> null
    }
}
