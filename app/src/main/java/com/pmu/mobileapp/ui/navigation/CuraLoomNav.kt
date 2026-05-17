package com.pmu.mobileapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pmu.mobileapp.CuraLoomApp
import com.pmu.mobileapp.R
import com.pmu.mobileapp.ui.feature.add.AddFeedScreen
import com.pmu.mobileapp.ui.feature.ai.AiScreen
import com.pmu.mobileapp.ui.feature.discover.DiscoverScreen
import com.pmu.mobileapp.ui.feature.episodes.EpisodeScreen
import com.pmu.mobileapp.ui.feature.home.HomeScreen
import com.pmu.mobileapp.ui.feature.library.LibraryScreen
import com.pmu.mobileapp.ui.feature.settings.SettingsScreen
import com.pmu.mobileapp.ui.feature.splash.SplashScreen
import com.pmu.mobileapp.ui.components.MiniNowPlayingBar

private object Routes {
    const val Splash = "splash"
    const val Home = "home"
    const val Discover = "discover"
    const val Ai = "ai"
    const val Add = "add"
    const val Library = "library"
    const val Settings = "settings"
    const val Episodes = "episodes/{feedId}"

    fun episodes(feedId: Long): String = "episodes/$feedId"
}

@Composable
fun CuraLoomNav(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as CuraLoomApp
    val playback = app.podcastPlayer.playbackState.collectAsStateWithLifecycle().value
    var dismissedEpisodeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val backStackEntry = nav.currentBackStackEntryAsState().value
    val currentRoute = backStackEntry?.destination?.route
    val hasBottomNavigation = currentRoute in setOf(
        Routes.Home,
        Routes.Discover,
        Routes.Ai,
        Routes.Library,
        Routes.Settings,
        Routes.Episodes
    )

    LaunchedEffect(playback.episodeId) {
        if (playback.episodeId == null) {
            dismissedEpisodeId = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.Splash) {
            composable(Routes.Splash) {
                SplashScreen {
                    nav.navigate(Routes.Home) { popUpTo(Routes.Splash) { inclusive = true } }
                }
            }
            composable(Routes.Home) {
                HomeScreen(
                    onAddFeed = { nav.navigate(Routes.Add) },
                    onOpenFeed = { nav.navigate(Routes.episodes(it)) },
                    onOpenLibrary = { nav.navigate(Routes.Library) },
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onOpenDiscover = { nav.navigate(Routes.Discover) },
                    onOpenAi = { nav.navigate(Routes.Ai) }
                )
            }
            composable(Routes.Discover) {
                DiscoverScreen(
                    onGoHome = { nav.navigate(Routes.Home) },
                    onOpenLibrary = { nav.navigate(Routes.Library) },
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onOpenAi = { nav.navigate(Routes.Ai) },
                    onOpenFeed = { nav.navigate(Routes.episodes(it)) }
                )
            }
            composable(Routes.Ai) {
                AiScreen(
                    onGoHome = { nav.navigate(Routes.Home) },
                    onOpenDiscover = { nav.navigate(Routes.Discover) },
                    onOpenLibrary = { nav.navigate(Routes.Library) },
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onOpenFeed = { nav.navigate(Routes.episodes(it)) }
                )
            }
            composable(Routes.Library) {
                LibraryScreen(
                    onGoHome = { nav.navigate(Routes.Home) },
                    onOpenDiscover = { nav.navigate(Routes.Discover) },
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onOpenAi = { nav.navigate(Routes.Ai) },
                    onOpenFeed = { nav.navigate(Routes.episodes(it)) }
                )
            }
            composable(Routes.Add) {
                AddFeedScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    onBack = { nav.popBackStack() },
                    onGoHome = { nav.navigate(Routes.Home) },
                    onOpenDiscover = { nav.navigate(Routes.Discover) },
                    onOpenLibrary = { nav.navigate(Routes.Library) },
                    onOpenAi = { nav.navigate(Routes.Ai) }
                )
            }
            composable(
                route = Routes.Episodes,
                arguments = listOf(navArgument("feedId") { type = NavType.LongType })
            ) { entry ->
                EpisodeScreen(
                    feedId = entry.arguments?.getLong("feedId") ?: 1L,
                    onBack = { nav.popBackStack() },
                    onOpenSettings = { nav.navigate(Routes.Settings) },
                    onGoHome = { nav.navigate(Routes.Home) },
                    onOpenDiscover = { nav.navigate(Routes.Discover) },
                    onOpenLibrary = { nav.navigate(Routes.Library) },
                    onOpenAi = { nav.navigate(Routes.Ai) }
                )
            }
        }

        if (playback.episodeId != null && playback.episodeId != dismissedEpisodeId && currentRoute != Routes.Splash) {
            MiniNowPlayingBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = if (hasBottomNavigation) 96.dp else 12.dp),
                playback = playback,
                onOpenLibrary = {
                    nav.navigate(Routes.Library) {
                        launchSingleTop = true
                    }
                },
                onTogglePlayPause = { app.podcastPlayer.togglePlayPause() },
                onSeekTo = { app.podcastPlayer.seekTo(it) },
                onToggleMute = { app.podcastPlayer.toggleMute() },
                onDecreaseVolume = { app.podcastPlayer.decreaseVolume() },
                onIncreaseVolume = { app.podcastPlayer.increaseVolume() },
                onDismiss = { dismissedEpisodeId = playback.episodeId }
            )
        } else if (playback.episodeId != null && currentRoute != Routes.Splash) {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .padding(bottom = if (hasBottomNavigation) 96.dp else 16.dp),
                onClick = { dismissedEpisodeId = null },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.show_now_playing)) }
            )
        }
    }
}
