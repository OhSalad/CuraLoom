package com.pmu.mobileapp.player

import kotlinx.coroutines.flow.StateFlow

interface PodcastPlayer {
    val playbackState: StateFlow<PlaybackState>

    fun play(
        episodeId: Long,
        episodeTitle: String,
        feedTitle: String,
        audioUrl: String?
    )

    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun toggleMute()
    fun decreaseVolume()
    fun pause()
    fun stop()
    fun release()
}
