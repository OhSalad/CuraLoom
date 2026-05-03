package com.pmu.mobileapp.ui.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.BuildConfig
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.R
import com.pmu.mobileapp.ui.components.PrimaryBottomNav
import com.pmu.mobileapp.ui.components.PrimaryNavItem
import com.pmu.mobileapp.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    onGoHome: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFeed: (Long) -> Unit,
    vm: AiViewModel = aiViewModel()
) {
    val state = vm.uiState

    Scaffold(
        topBar = { AiTopBar() },
        bottomBar = {
            PrimaryBottomNav(
                selectedItem = PrimaryNavItem.Ai,
                onGoHome = onGoHome,
                onOpenDiscover = onOpenDiscover,
                onOpenAi = {},
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!state.isAiConfigured) {
                MissingApiKeyCard(Modifier.padding(16.dp))
            } else {
                ChatHistory(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onRetry = {
                        when (state.error?.type) {
                            AiUiErrorType.NoSelectedFeeds,
                            AiUiErrorType.NoLocalEpisodes -> vm.buildEpisodeList()
                            else -> vm.recommendFeeds()
                        }
                    },
                    onFollow = vm::followFeed,
                    onOpenFeed = onOpenFeed,
                    onToggleSelected = vm::toggleFeedSelection,
                    onBuildEpisodeList = { vm.buildEpisodeList() },
                    onPlayFirst = vm::playFirstEpisode,
                    onPlayEpisode = vm::playEpisode,
                    onPromptSelected = vm::onPromptChange
                )
                ChatInput(
                    state = state,
                    onPromptChange = vm::onPromptChange,
                    onSend = { vm.recommendFeeds() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.ai_nav_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.ai_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun aiViewModel(): AiViewModel {
    val context = LocalContext.current
    val app = context.applicationContext as CuraLoomApp
    val factory = remember(app, context) {
        AiViewModel.Factory(
            aiService = app.appContainer.podcastAiService,
            libraryGateway = DefaultAiLibraryGateway(
                feedService = app.appContainer.feedService,
                repository = app.podcastRepository,
                player = app.podcastPlayer
            ),
            isAiConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            localeTagProvider = { LocaleHelper.getLanguage(context) }
        )
    }
    return viewModel(factory = factory)
}

@Composable
private fun ChatHistory(
    modifier: Modifier,
    state: AiUiState,
    onRetry: () -> Unit,
    onFollow: (String) -> Unit,
    onOpenFeed: (Long) -> Unit,
    onToggleSelected: (String) -> Unit,
    onBuildEpisodeList: () -> Unit,
    onPlayFirst: () -> Unit,
    onPlayEpisode: (Long) -> Unit,
    onPromptSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        if (state.submittedPrompt.isBlank() && state.summary.isBlank() && state.error == null) {
            item {
                AssistantBubble {
                    Text(stringResource(R.string.ai_chat_welcome))
                }
            }
            item {
                PromptSuggestions(onPromptSelected = onPromptSelected)
            }
        }

        if (state.submittedPrompt.isNotBlank()) {
            item {
                UserBubble {
                    Text(state.submittedPrompt)
                }
            }
        }

        if (state.isLoadingRecommendations) {
            item {
                AssistantBubble {
                    LoadingBubbleContent(text = stringResource(R.string.ai_recommendations_loading))
                }
            }
        }

        state.error?.let { error ->
            item {
                AssistantBubble {
                    Text(error.message(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.ai_retry))
                    }
                }
            }
        }

        if (state.summary.isNotBlank()) {
            item {
                AssistantBubble {
                    Text(state.summary)
                }
            }
        }

        if (!state.isLoadingRecommendations && state.summary.isNotBlank() && state.recommendations.isEmpty()) {
            item {
                AssistantBubble {
                    Text(stringResource(R.string.ai_empty_recommendations))
                }
            }
        }

        if (state.recommendations.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.ai_recommendations_heading),
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.recommendations, key = { it.feed.url }) { recommendation ->
                RecommendationCard(
                    item = recommendation,
                    onFollow = { onFollow(recommendation.feed.url) },
                    onOpenFeed = { recommendation.localFeedId?.let(onOpenFeed) },
                    onToggleSelected = { onToggleSelected(recommendation.feed.url) }
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedFeedUrls.isNotEmpty() && !state.isBuildingEpisodeList,
                    onClick = onBuildEpisodeList
                ) {
                    Text(
                        if (state.isBuildingEpisodeList) {
                            stringResource(R.string.ai_episode_list_loading)
                        } else {
                            stringResource(R.string.ai_generate_episode_list)
                        }
                    )
                }
            }
        }

        if (state.isBuildingEpisodeList) {
            item {
                AssistantBubble {
                    LoadingBubbleContent(text = stringResource(R.string.ai_episode_list_loading))
                }
            }
        }

        if (state.playlistTitle.isNotBlank() || state.plannedEpisodes.isNotEmpty()) {
            item {
                EpisodePlanHeader(
                    state = state,
                    onPlayFirst = onPlayFirst
                )
            }
            items(state.plannedEpisodes, key = { it.episode.id }) { item ->
                PlannedEpisodeCard(
                    item = item,
                    onPlayEpisode = { onPlayEpisode(item.episode.id) },
                    onOpenFeed = { onOpenFeed(item.feedId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptSuggestions(onPromptSelected: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(R.string.ai_suggestion_science),
        stringResource(R.string.ai_suggestion_tech),
        stringResource(R.string.ai_suggestion_history)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onPromptSelected(suggestion) },
                label = { Text(suggestion) }
            )
        }
    }
}

@Composable
private fun ChatInput(
    state: AiUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val isWorking = state.isLoadingRecommendations || state.isBuildingEpisodeList
    Surface(
        modifier = Modifier.imePadding(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 112.dp),
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text(stringResource(R.string.ai_prompt_hint)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (!isWorking && state.prompt.isNotBlank()) {
                            onSend()
                        }
                    }
                )
            )
            FilledIconButton(
                modifier = Modifier.size(48.dp),
                enabled = !isWorking && state.prompt.isNotBlank(),
                onClick = onSend
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.ai_send))
            }
        }
    }
}

@Composable
private fun UserBubble(content: @Composable ColumnScope.() -> Unit) {
    ChatBubble(isUser = true, content = content)
}

@Composable
private fun AssistantBubble(content: @Composable ColumnScope.() -> Unit) {
    ChatBubble(isUser = false, content = content)
}

@Composable
private fun ChatBubble(
    isUser: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

@Composable
private fun LoadingBubbleContent(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text)
    }
}

@Composable
private fun MissingApiKeyCard(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.ai_missing_key_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.ai_missing_key_body))
        }
    }
}

@Composable
private fun AiUiError.message(): String =
    when (type) {
        AiUiErrorType.EmptyPrompt -> stringResource(R.string.ai_error_empty_prompt)
        AiUiErrorType.MissingApiKey -> stringResource(R.string.ai_missing_key_body)
        AiUiErrorType.Network -> stringResource(R.string.ai_error_network)
        AiUiErrorType.RateLimited -> stringResource(R.string.ai_error_rate_limited)
        AiUiErrorType.MalformedResponse -> stringResource(R.string.ai_error_malformed)
        AiUiErrorType.NoSelectedFeeds -> stringResource(R.string.ai_error_no_selected_feeds)
        AiUiErrorType.NoLocalEpisodes -> stringResource(R.string.ai_error_no_local_episodes)
        AiUiErrorType.Unknown -> stringResource(R.string.ai_error_unknown)
    }

@Composable
private fun RecommendationCard(
    item: AiRecommendedFeed,
    onFollow: () -> Unit,
    onOpenFeed: () -> Unit,
    onToggleSelected: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.feed.title, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.feed.category} - ${item.feed.author}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("#${item.rank}", style = MaterialTheme.typography.bodySmall)
            }
            Text(item.reason)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.isFollowed) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenFeed
                    ) {
                        Text(stringResource(R.string.ai_open_episodes))
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onFollow,
                        enabled = !item.isSyncing
                    ) {
                        Text(
                            if (item.isSyncing) {
                                stringResource(R.string.following_syncing)
                            } else {
                                stringResource(R.string.follow)
                            }
                        )
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleSelected
                ) {
                    Text(
                        if (item.isSelected) {
                            stringResource(R.string.ai_selected_for_episode_list)
                        } else {
                            stringResource(R.string.ai_use_for_episode_list)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodePlanHeader(state: AiUiState, onPlayFirst: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = state.playlistTitle.ifBlank { stringResource(R.string.ai_episode_list_heading) },
            fontWeight = FontWeight.SemiBold
        )
        if (state.playlistReason.isNotBlank()) {
            Text(state.playlistReason)
        }
        Button(
            enabled = state.plannedEpisodes.any { !it.episode.audioUrl.isNullOrBlank() },
            onClick = onPlayFirst
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ai_play_first))
        }
    }
}

@Composable
private fun PlannedEpisodeCard(
    item: AiPlannedEpisodeUi,
    onPlayEpisode: () -> Unit,
    onOpenFeed: () -> Unit
) {
    val hasAudio = !item.episode.audioUrl.isNullOrBlank()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${item.rank}. ${item.episode.title}", fontWeight = FontWeight.SemiBold)
            Text(item.feedTitle, style = MaterialTheme.typography.bodySmall)
            Text(item.episodeReason)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.episode.duration ?: if (hasAudio) "" else stringResource(R.string.no_audio))
                TextButton(onClick = onOpenFeed) {
                    Text(stringResource(R.string.view_feed))
                }
            }
            Button(onClick = onPlayEpisode, enabled = hasAudio) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_play_this_episode))
            }
        }
    }
}
