package com.pmu.mobileapp.ui.feature.settings

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pmu.mobileapp.R
import com.pmu.mobileapp.ui.components.PrimaryBottomNav
import com.pmu.mobileapp.ui.components.PrimaryNavItem
import com.pmu.mobileapp.ui.components.ToggleRow
import com.pmu.mobileapp.util.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenAi: () -> Unit,
    vm: SettingsViewModel = viewModel()
    ) {
        val context = LocalContext.current
        val state = vm.uiState
        val isArabic = LocaleHelper.isArabic(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }
            )
        },
        bottomBar = {
            PrimaryBottomNav(
                selectedItem = PrimaryNavItem.Settings,
                onGoHome = onGoHome,
                onOpenDiscover = onOpenDiscover,
                onOpenAi = onOpenAi,
                onOpenLibrary = onOpenLibrary,
                onOpenSettings = {}
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings_subtitle))
            LanguageSwitchCard(
                isArabic = isArabic,
                onToggle = {
                    LocaleHelper.setLocale(context, if (isArabic) "en" else "ar")
                    (context as? ComponentActivity)?.recreate()
                }
            )
            ToggleRow(stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_caption), darkMode) {
                onDarkModeChange(it)
                Toast.makeText(
                    context,
                    if (it) context.getString(R.string.dark_mode_on) else context.getString(R.string.dark_mode_off),
                    Toast.LENGTH_SHORT
                ).show()
            }
            Text(stringResource(R.string.storage), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.storage_info, state.feedCount))
        }
    }
}

@Composable
private fun LanguageSwitchCard(
    isArabic: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold)
                Text(
                    if (isArabic) stringResource(R.string.language_switch_to_english) else stringResource(R.string.language_switch_to_arabic),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = if (isArabic) "AR" else "EN",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
