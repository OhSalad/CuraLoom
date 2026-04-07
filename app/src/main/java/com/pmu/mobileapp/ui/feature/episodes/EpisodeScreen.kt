package com.pmu.mobileapp.ui.feature.episodes

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.R
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.player.PlaybackStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(
    feedId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoHome: () -> Unit,
    onOpenLibrary: () -> Unit,
    vm: EpisodeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = vm.uiState
    val playback = vm.playbackState.collectAsStateWithLifecycle().value

    LaunchedEffect(feedId) {
        vm.initialize(feedId)
    }

    val initializedForRoute = state.feedId == feedId
    if (initializedForRoute && state.feed == null && !state.isLoading) {
        LaunchedEffect(state.feedId, state.feed) {
            Toast.makeText(context, context.getString(R.string.feed_not_found), Toast.LENGTH_SHORT).show()
            onBack()
        }
        return
    }
    val feed = state.feed ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(feed.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    TextButton(onClick = {
                        vm.toggleFollowing()
                        Toast.makeText(
                            context,
                            if (vm.uiState.following) context.getString(R.string.following_enabled) else context.getString(R.string.following_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) { Text(if (state.following) stringResource(R.string.unfollow) else stringResource(R.string.follow)) }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, feed.title, feed.url))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_via)))
                    }) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = onGoHome, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.home)) })
                NavigationBarItem(selected = true, onClick = onOpenLibrary, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(stringResource(R.string.manage_library)) })
                NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        EpisodeContent(
            modifier = Modifier.padding(padding).padding(16.dp),
            state = state,
            playbackEpisodeId = playback.episodeId,
            playbackStatus = playback.status,
            onFilterSelected = vm::setFilter,
            onPlayEpisode = {
                vm.playEpisode(it)
                Toast.makeText(context, context.getString(R.string.now_playing, it.title), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeContent(
    modifier: Modifier,
    state: EpisodeUiState,
    playbackEpisodeId: Long?,
    playbackStatus: PlaybackStatus,
    onFilterSelected: (String) -> Unit,
    onPlayEpisode: (Episode) -> Unit
) {
    Column(modifier) {
        val episodeCountText = stringResource(R.string.episodes_count, state.episodes.size)
        Text(episodeCountText, style = MaterialTheme.typography.bodySmall)
        val description = state.feed?.description.orEmpty()
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (state.isLoading) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.loading_feed))
        }
        if (!state.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.feed_sync_failed, state.errorMessage ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.filter == FILTER_ALL, onClick = { onFilterSelected(FILTER_ALL) }, label = { Text(stringResource(R.string.filter_all)) })
            FilterChip(selected = state.filter == FILTER_UNPLAYED, onClick = { onFilterSelected(FILTER_UNPLAYED) }, label = { Text(stringResource(R.string.filter_unplayed)) })
            FilterChip(selected = state.filter == FILTER_POPULAR, onClick = { onFilterSelected(FILTER_POPULAR) }, label = { Text(stringResource(R.string.filter_popular)) })
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 170.dp)) {
            items(state.episodes, key = { it.id }) { episode ->
                val isCurrentEpisode = playbackEpisodeId == episode.id
                val showPause = isCurrentEpisode && playbackStatus == PlaybackStatus.PLAYING
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                episode.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(episode.duration ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onPlayEpisode(episode) }) {
                            Icon(if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        }
                    }
                }
            }
        }
    }
}
