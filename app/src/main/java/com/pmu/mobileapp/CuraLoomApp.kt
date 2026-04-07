package com.pmu.mobileapp

import android.app.Application
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.data.repository.DefaultPodcastRepository
import com.pmu.mobileapp.data.repository.PodcastRepository
import com.pmu.mobileapp.player.ExoPodcastPlayer
import com.pmu.mobileapp.player.PlaybackStatus
import com.pmu.mobileapp.player.PodcastPlayer
import com.pmu.mobileapp.util.ThemeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CuraLoomApp : Application() {
    lateinit var podcastRepository: PodcastRepository
        private set

    lateinit var podcastPlayer: PodcastPlayer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var playbackSyncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ThemeHelper.applySavedTheme(this)
        val dbHelper = DatabaseHelper.getInstance(this)
        podcastRepository = DefaultPodcastRepository(
            dbHelper = dbHelper,
            feedService = dbHelper.feedService,
            episodeService = dbHelper.episodeService,
            libraryService = dbHelper.libraryService
        )
        podcastPlayer = ExoPodcastPlayer(this)
        startPlaybackProgressSync()
    }

    override fun onTerminate() {
        playbackSyncJob?.cancel()
        appScope.cancel()
        podcastPlayer.release()
        super.onTerminate()
    }

    private fun startPlaybackProgressSync() {
        playbackSyncJob?.cancel()
        playbackSyncJob = appScope.launch {
            var lastSavedEpisodeId: Long? = null
            var lastSavedSecond = -1L

            podcastPlayer.playbackState.collect { state ->
                val episodeId = state.episodeId ?: return@collect
                val targetPositionMs = if (state.status == PlaybackStatus.ENDED) 0L else state.positionMs.coerceAtLeast(0L)
                val targetSecond = targetPositionMs / 1000L
                val shouldSave =
                    state.status == PlaybackStatus.ENDED ||
                        episodeId != lastSavedEpisodeId ||
                        targetSecond != lastSavedSecond

                if (!shouldSave) return@collect

                podcastRepository.updateEpisodePlaybackPosition(episodeId, targetPositionMs)
                lastSavedEpisodeId = episodeId
                lastSavedSecond = targetSecond
            }
        }
    }
}
