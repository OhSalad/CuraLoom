package com.pmu.mobileapp.player

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

data class PlaybackState(
    val episodeId: Long? = null,
    val episodeTitle: String = "",
    val feedTitle: String = "",
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val isMuted: Boolean = false
)
