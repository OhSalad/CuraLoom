package com.pmu.mobileapp.ui.feature.library

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.R
import com.pmu.mobileapp.model.LibraryEpisode
import com.pmu.mobileapp.player.PlaybackStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onGoHome: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFeed: (Long) -> Unit,
    vm: LibraryViewModel = viewModel()
) {
    val state = vm.uiState
    val playback = vm.playbackState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_heading)) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = onGoHome, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.home)) })
                NavigationBarItem(selected = false, onClick = onOpenDiscover, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text(stringResource(R.string.discover)) })
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(stringResource(R.string.manage_library)) })
                NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.filter == LibraryFilters.All, onClick = { vm.setFilter(LibraryFilters.All) }, label = { Text(stringResource(R.string.filter_all)) })
                FilterChip(selected = state.filter == LibraryFilters.Played, onClick = { vm.setFilter(LibraryFilters.Played) }, label = { Text(stringResource(R.string.filter_played)) })
                FilterChip(selected = state.filter == LibraryFilters.Unplayed, onClick = { vm.setFilter(LibraryFilters.Unplayed) }, label = { Text(stringResource(R.string.filter_unplayed)) })
            }
            Spacer(Modifier.height(10.dp))
            if (state.episodes.isEmpty()) {
                Text(stringResource(R.string.library_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 72.dp)) {
                    items(state.episodes, key = { it.id }) { item ->
                        LibraryEpisodeCard(
                            item = item,
                            isCurrent = playback.episodeId == item.id,
                            isPlaying = playback.status == PlaybackStatus.PLAYING,
                            onPlay = { vm.playEpisode(item) },
                            onOpenFeed = { onOpenFeed(item.feedId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEpisodeCard(
    item: LibraryEpisode,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onOpenFeed: () -> Unit
) {
    val hasAudio = !item.audioUrl.isNullOrBlank()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(item.feedTitle)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.duration ?: if (hasAudio) "" else stringResource(R.string.no_audio))
                TextButtonAction(text = stringResource(R.string.view_feed), onClick = onOpenFeed)
            }
            Button(onClick = onPlay, enabled = hasAudio) {
                val showPause = isCurrent && isPlaying
                Icon(if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text(
                    when {
                        !hasAudio -> stringResource(R.string.no_audio)
                        showPause -> stringResource(R.string.pause_episode)
                        else -> stringResource(R.string.play_episode)
                    }
                )
            }
        }
    }
}

@Composable
private fun TextButtonAction(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text)
    }
}
