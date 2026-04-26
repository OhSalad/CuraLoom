package com.pmu.mobileapp.ui.feature.discover

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.R
import com.pmu.mobileapp.data.DefaultPodcastFeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onGoHome: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFeed: (Long) -> Unit,
    vm: DiscoverViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = vm.uiState

    LaunchedEffect(state.lastFollowedTitle) {
        val title = state.lastFollowedTitle ?: return@LaunchedEffect
        Toast.makeText(context, context.getString(R.string.feed_followed, title), Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.discover_title)) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = onGoHome, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.home)) })
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text(stringResource(R.string.discover)) })
                NavigationBarItem(selected = false, onClick = onOpenLibrary, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(stringResource(R.string.manage_library)) })
                NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.discover_subtitle))
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(state.feeds, key = { it.url }) { feed ->
                    val isFollowed = state.followedUrls.contains(feed.url)
                    val isSyncing = state.syncingUrls.contains(feed.url)
                    DiscoverFeedCard(
                        feed = feed,
                        isFollowed = isFollowed,
                        isSyncing = isSyncing,
                        onFollow = { vm.follow(feed) },
                        onOpenFeed = {
                            val app = context.applicationContext as CuraLoomApp
                            val followedFeed = app.appContainer.feedService.getFeedByUrl(feed.url)
                            if (followedFeed != null) onOpenFeed(followedFeed.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverFeedCard(
    feed: DefaultPodcastFeed,
    isFollowed: Boolean,
    isSyncing: Boolean,
    onFollow: () -> Unit,
    onOpenFeed: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isFollowed, onClick = onOpenFeed)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(feed.title, fontWeight = FontWeight.Bold)
                    Text(feed.category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = onFollow, enabled = !isFollowed && !isSyncing) {
                    Text(
                        when {
                            isFollowed -> stringResource(R.string.following)
                            isSyncing -> stringResource(R.string.following_syncing)
                            else -> stringResource(R.string.follow)
                        }
                    )
                }
            }
            Text(feed.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (isFollowed) {
                TextButton(onClick = onOpenFeed) {
                    Text(stringResource(R.string.view_episodes))
                }
            }
        }
    }
}
