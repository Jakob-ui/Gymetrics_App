package com.laschober.gymetrics.ui.main.templates

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laschober.gymetrics.data.remote.dto.GenerationStatus
import com.laschober.gymetrics.data.remote.dto.TemplateOverviewResponseDto
import com.laschober.gymetrics.ui.components.PullToRefreshBoxCompat
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun TemplateScreen(
    viewModel: TemplateScreenViewModel = koinViewModel(),
    onTemplateClick: (id: String, title: String) -> Unit = { _, _ -> },
    onAddClick: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    bottomPadding: Dp = 130.dp,
) {
    LaunchedEffect(viewModel.transientError) {
        viewModel.transientError?.let {
            onShowMessage(it)
            viewModel.consumeTransientError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.query,
            onValueChange = viewModel::updateQuery,
            label = { Text("Search") },
            leadingIcon = { Icon(FeatherIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (viewModel.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateQuery("") }) {
                        Icon(FeatherIcons.X, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            TemplateSortOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = viewModel.sortOption == option,
                    onClick = { viewModel.selectSort(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, TemplateSortOption.entries.size),
                ) {
                    Text(option.label)
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(3.dp)) {
            if (viewModel.reloading && viewModel.state is TemplateState.Success) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        PullToRefreshBoxCompat(
            isRefreshing = viewModel.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
        when (val s = viewModel.state) {
            TemplateState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is TemplateState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { viewModel.load() }) { Text("Try again") }
            }

            is TemplateState.Success ->
                if (s.templates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No templates yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val hasScrolled = listState.firstVisibleItemIndex > 0 ||
                                listState.firstVisibleItemScrollOffset > 0
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            hasScrolled && lastVisible >= s.templates.size - 3
                        }
                    }

                    LaunchedEffect(shouldLoadMore, s.templates.size) {
                        if (shouldLoadMore) viewModel.loadNextPage()
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (viewModel.reloading) 0.55f else 1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.templates, key = { it.id }) { template ->
                            TemplateCard(template, onClick = { onTemplateClick(template.id, template.title) })
                        }
                        if (viewModel.loadingMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
        }

        AddTemplates(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp).padding(bottom = 160.dp),
        )
        }
    }
}

@Composable
private fun TemplateCard(template: TemplateOverviewResponseDto, onClick: () -> Unit) {
    val generating = template.generationStatus == GenerationStatus.GENERATING
    val failed = template.generationStatus == GenerationStatus.FAILED

    Card(
        onClick = onClick,
        enabled = !generating,
        modifier = Modifier.fillMaxWidth().alpha(if (generating) 0.6f else 1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (failed) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    generating -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    failed -> Icon(
                        imageVector = FeatherIcons.AlertTriangle,
                        contentDescription = "Generation failed",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    else -> Text(
                        text = template.title.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = when {
                        generating -> "Generating with AI…"
                        failed -> "AI generation failed"
                        else -> template.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!generating && !failed && template.description.isNotBlank()) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (failed) {
                    Text(
                        text = "Tap to remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (!generating) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FeatherIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AddTemplates(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(FeatherIcons.Plus, contentDescription = null) },
        text = { Text("Add Template") },
        modifier = modifier,
    )
}