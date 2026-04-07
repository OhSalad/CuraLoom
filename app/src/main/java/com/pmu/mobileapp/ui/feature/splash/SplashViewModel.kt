package com.pmu.mobileapp.ui.feature.splash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    var progress by mutableIntStateOf(0)
        private set
    var done by mutableIntStateOf(0)
        private set

    init {
        startProgress()
    }

    private fun startProgress() {
        viewModelScope.launch {
            while (progress < 100) {
                progress += if (progress < 30) 3 else if (progress < 70) 2 else 1
                if (progress > 100) progress = 100
                delay(45)
            }
            delay(250)
            done = 1
        }
    }
}
