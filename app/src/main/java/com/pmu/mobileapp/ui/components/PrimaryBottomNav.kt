package com.pmu.mobileapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pmu.mobileapp.R

enum class PrimaryNavItem {
    Home,
    Discover,
    Ai,
    Library,
    Settings
}

@Composable
fun PrimaryBottomNav(
    selectedItem: PrimaryNavItem,
    onGoHome: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == PrimaryNavItem.Home,
            onClick = onGoHome,
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text(stringResource(R.string.home)) }
        )
        NavigationBarItem(
            selected = selectedItem == PrimaryNavItem.Discover,
            onClick = onOpenDiscover,
            icon = { Icon(Icons.Default.PlayArrow, null) },
            label = { Text(stringResource(R.string.discover)) }
        )
        NavigationBarItem(
            selected = selectedItem == PrimaryNavItem.Ai,
            onClick = onOpenAi,
            icon = { Icon(Icons.Default.AutoAwesome, null) },
            label = { Text(stringResource(R.string.ai_nav_label)) }
        )
        NavigationBarItem(
            selected = selectedItem == PrimaryNavItem.Library,
            onClick = onOpenLibrary,
            icon = { Icon(Icons.Default.LibraryMusic, null) },
            label = { Text(stringResource(R.string.manage_library)) }
        )
        NavigationBarItem(
            selected = selectedItem == PrimaryNavItem.Settings,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text(stringResource(R.string.settings)) }
        )
    }
}
