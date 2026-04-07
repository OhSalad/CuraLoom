package com.pmu.mobileapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pmu.mobileapp.ui.navigation.CuraLoomNav
import com.pmu.mobileapp.util.LocaleHelper
import com.pmu.mobileapp.util.ThemeHelper

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDark = ThemeHelper.isDarkModeEnabled(this)
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(initialDark) }
            MaterialTheme(
                colorScheme = if (darkMode) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CuraLoomNav(
                        darkMode = darkMode,
                        onDarkModeChange = {
                            darkMode = it
                            ThemeHelper.setDarkModeEnabled(this, it)
                        }
                    )
                }
            }
        }
    }
}
