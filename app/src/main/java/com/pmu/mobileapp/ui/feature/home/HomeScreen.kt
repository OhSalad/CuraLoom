package com.pmu.mobileapp.ui.feature.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.R
import com.pmu.mobileapp.model.Feed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAddFeed: () -> Unit,
    onOpenFeed: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = vm.uiState

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.reloadOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.renameTarget != null) {
        RenameFeedDialog(
            text = state.renameText,
            onTextChange = vm::onRenameTextChange,
            onConfirm = vm::confirmRename,
            onDismiss = vm::cancelRename
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_heading)) }) },
        bottomBar = {
            HomeBottomBar(
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = onOpenSettings,
                onDiscover = { Toast.makeText(context, context.getString(R.string.discover_coming_soon), Toast.LENGTH_SHORT).show() }
            )
        },
        floatingActionButton = { IconButton(onClick = onAddFeed) { Icon(Icons.Default.Add, stringResource(R.string.add_feed_title)) } }
    ) { padding ->
        HomeContent(
            modifier = Modifier.padding(padding),
            state = state,
            onQueryChange = vm::onQueryChange,
            onCategorySelected = vm::onCategorySelected,
            onOpenFeed = onOpenFeed,
            onToggleFeedMenu = vm::toggleFeedMenu,
            onRenameFeed = vm::beginRename,
            onRefreshFeed = vm::refreshFeed,
            onDeleteFeed = {
                vm.deleteFeed(it)
                Toast.makeText(context, context.getString(R.string.feed_deleted), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun RenameFeedDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_feed)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.title_hint)) }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun HomeBottomBar(onOpenLibrary: () -> Unit, onOpenSettings: () -> Unit, onDiscover: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.home)) })
        NavigationBarItem(selected = false, onClick = onDiscover, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text(stringResource(R.string.discover)) })
        NavigationBarItem(selected = false, onClick = onOpenLibrary, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text(stringResource(R.string.manage_library)) })
        NavigationBarItem(selected = false, onClick = onOpenSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    modifier: Modifier,
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onOpenFeed: (Long) -> Unit,
    onToggleFeedMenu: (Long) -> Unit,
    onRenameFeed: (Feed) -> Unit,
    onRefreshFeed: (Feed) -> Unit,
    onDeleteFeed: (Feed) -> Unit
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_hint)) }
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.selectedCategory == "All",
                onClick = { onCategorySelected("All") },
                label = { Text(stringResource(R.string.category_all)) }
            )
            state.categories.forEach { c ->
                FilterChip(selected = c == state.selectedCategory, onClick = { onCategorySelected(c) }, label = { Text(c) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.collections_followed, state.feedCount))
        Spacer(Modifier.height(8.dp))
        val continueFeed = state.feeds.firstOrNull()
        if (continueFeed != null) {
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenFeed(continueFeed.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.continue_listening), fontWeight = FontWeight.SemiBold)
                    Text(continueFeed.title)
                    Text(continueFeed.author ?: stringResource(R.string.continue_meta))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FeedList(
            feeds = state.feeds,
            menuFeedId = state.menuFeedId,
            onOpenFeed = onOpenFeed,
            onToggleFeedMenu = onToggleFeedMenu,
            onRenameFeed = onRenameFeed,
            onRefreshFeed = onRefreshFeed,
            onDeleteFeed = onDeleteFeed
        )
    }
}

@Composable
private fun FeedList(
    feeds: List<Feed>,
    menuFeedId: Long,
    onOpenFeed: (Long) -> Unit,
    onToggleFeedMenu: (Long) -> Unit,
    onRenameFeed: (Feed) -> Unit,
    onRefreshFeed: (Feed) -> Unit,
    onDeleteFeed: (Feed) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 72.dp)) {
        items(feeds, key = { it.id }) { feed ->
            Card(Modifier.fillMaxWidth().clickable { onToggleFeedMenu(feed.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(feed.title, fontWeight = FontWeight.Bold)
                            Text(feed.category ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onOpenFeed(feed.id) }) { Text(stringResource(R.string.view_episodes)) }
                    }
                    if (menuFeedId == feed.id) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onRenameFeed(feed) }) { Text(stringResource(R.string.rename_feed)) }
                            TextButton(onClick = { onRefreshFeed(feed) }) { Text(stringResource(R.string.refresh_feed)) }
                            TextButton(onClick = { onDeleteFeed(feed) }) { Text(stringResource(R.string.delete_feed)) }
                        }
                    }
                }
            }
        }
    }
}
