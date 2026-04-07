package com.pmu.mobileapp.ui.feature.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pmu.mobileapp.data.DatabaseHelper

data class SettingsUiState(
    val newEpisodesEnabled: Boolean = true,
    val personalizedEnabled: Boolean = true,
    val feedCount: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseHelper.getInstance(application)
    var uiState by mutableStateOf(SettingsUiState(feedCount = db.getFeedCount()))
        private set

    fun toggleNewEpisodes(value: Boolean) {
        uiState = uiState.copy(newEpisodesEnabled = value)
    }

    fun togglePersonalized(value: Boolean) {
        uiState = uiState.copy(personalizedEnabled = value)
    }

    fun refreshStorage() {
        uiState = uiState.copy(feedCount = db.getFeedCount())
    }
}
