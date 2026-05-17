package com.pmu.mobileapp

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
        val initialMaterialYouColors = ThemeHelper.isMaterialYouColorsEnabled(this)
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(initialDark) }
            var materialYouColors by rememberSaveable { mutableStateOf(initialMaterialYouColors) }
            val colorScheme = when {
                materialYouColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkMode -> dynamicDarkColorScheme(this)
                materialYouColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(this)
                darkMode -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CuraLoomNav(
                        darkMode = darkMode,
                        materialYouColors = materialYouColors,
                        onDarkModeChange = {
                            darkMode = it
                            ThemeHelper.setDarkModeEnabled(this, it)
                        },
                        onMaterialYouColorsChange = {
                            materialYouColors = it
                            ThemeHelper.setMaterialYouColorsEnabled(this, it)
                        }
                    )
                }
            }
        }
    }
}
