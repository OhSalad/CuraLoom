package com.pmu.mobileapp.ui.feature.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pmu.mobileapp.CuraLoomApp

data class SettingsUiState(
    val feedCount: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val feedService = (application as CuraLoomApp).appContainer.feedService
    var uiState by mutableStateOf(SettingsUiState(feedCount = feedService.getFeedCount()))
        private set

    fun refreshStorage() {
        uiState = uiState.copy(feedCount = feedService.getFeedCount())
    }
}
