package com.pmu.mobileapp

import android.app.Application
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.data.repository.DefaultPodcastRepository
import com.pmu.mobileapp.data.repository.PodcastRepository
import com.pmu.mobileapp.player.ExoPodcastPlayer
import com.pmu.mobileapp.player.PodcastPlayer
import com.pmu.mobileapp.util.ThemeHelper

class CuraLoomApp : Application() {
    lateinit var podcastRepository: PodcastRepository
        private set

    lateinit var podcastPlayer: PodcastPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        ThemeHelper.applySavedTheme(this)
        podcastRepository = DefaultPodcastRepository(DatabaseHelper.getInstance(this))
        podcastPlayer = ExoPodcastPlayer(this)
    }

    override fun onTerminate() {
        podcastPlayer.release()
        super.onTerminate()
    }
}
