package com.pmu.mobileapp.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class ExoPodcastPlayer(context: Context) : PodcastPlayer {
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("CuraLoom/1.0")
        .setAllowCrossProtocolRedirects(true)
    private val exoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .build()
    private val _playbackState = MutableStateFlow(PlaybackState())
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressTicker = object : Runnable {
        override fun run() {
            publishProgressState()
            if (exoPlayer.isPlaying) {
                progressHandler.postDelayed(this, 500L)
            }
        }
    }

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentAudioUrl: String? = null
    private var lastVolumeBeforeMute: Float = 1f

    init {
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, true)

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val status = when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
                        Player.STATE_READY -> if (exoPlayer.isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                        Player.STATE_ENDED -> PlaybackStatus.ENDED
                        else -> PlaybackStatus.IDLE
                    }
                    _playbackState.value = _playbackState.value.copy(
                        status = status,
                        isPlaying = exoPlayer.isPlaying,
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 0L,
                        volume = exoPlayer.volume,
                        isMuted = exoPlayer.volume <= 0.001f
                    )
                    if (exoPlayer.isPlaying) startProgressTicker() else stopProgressTicker()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 0L,
                        volume = exoPlayer.volume,
                        isMuted = exoPlayer.volume <= 0.001f
                    )
                    if (isPlaying) startProgressTicker() else stopProgressTicker()
                }

                override fun onPlayerError(error: PlaybackException) {
                    stopProgressTicker()
                    exoPlayer.playWhenReady = false
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.ERROR,
                        isPlaying = false
                    )
                }
            }
        )
    }

    override fun play(
        episodeId: Long,
        episodeTitle: String,
        feedTitle: String,
        audioUrl: String?,
        startPositionMs: Long
    ) {
        if (audioUrl.isNullOrBlank()) {
            return
        }

        val sameEpisode = _playbackState.value.episodeId == episodeId
        val sameUrl = currentAudioUrl == audioUrl
        if (sameEpisode && sameUrl) {
            val hasPreparedMedia = exoPlayer.currentMediaItem != null && exoPlayer.playbackState != Player.STATE_IDLE
            if (!hasPreparedMedia) {
                exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
                exoPlayer.prepare()
                val resumePosition = startPositionMs.coerceAtLeast(0L)
                if (resumePosition > 0L) {
                    exoPlayer.seekTo(resumePosition)
                }
            } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            } else if (startPositionMs > 0L && !exoPlayer.isPlaying) {
                val boundedResume = if (exoPlayer.duration > 0L) {
                    startPositionMs.coerceIn(0L, exoPlayer.duration)
                } else {
                    startPositionMs.coerceAtLeast(0L)
                }
                exoPlayer.seekTo(boundedResume)
            }
            if (!exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }
            return
        }

        currentAudioUrl = audioUrl
        _playbackState.value = _playbackState.value.copy(
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            feedTitle = feedTitle,
            status = PlaybackStatus.BUFFERING,
            isPlaying = false
        )
        exoPlayer.setMediaItem(MediaItem.fromUri(audioUrl))
        exoPlayer.prepare()
        val resumePosition = startPositionMs.coerceAtLeast(0L)
        if (resumePosition > 0L) {
            exoPlayer.seekTo(resumePosition)
        }
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    override fun seekTo(positionMs: Long) {
        val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        val bounded = if (duration > 0) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        exoPlayer.seekTo(bounded)
        publishProgressState()
    }

    override fun seekBy(deltaMs: Long) {
        val current = exoPlayer.currentPosition.coerceAtLeast(0L)
        seekTo(current + deltaMs)
    }

    override fun toggleMute() {
        if (exoPlayer.volume <= 0.001f) {
            val restored = if (lastVolumeBeforeMute <= 0.001f) 1f else lastVolumeBeforeMute
            exoPlayer.volume = restored
        } else {
            lastVolumeBeforeMute = exoPlayer.volume
            exoPlayer.volume = 0f
        }
        publishProgressState()
    }

    override fun decreaseVolume() {
        val current = exoPlayer.volume
        if (current <= 0.001f) {
            publishProgressState()
            return
        }
        val next = (current - 0.1f).coerceAtLeast(0f)
        exoPlayer.volume = next
        if (next > 0f) {
            lastVolumeBeforeMute = next
        }
        publishProgressState()
    }

    override fun increaseVolume() {
        val next = (exoPlayer.volume + 0.1f).coerceAtMost(1f)
        exoPlayer.volume = next
        if (next > 0f) {
            lastVolumeBeforeMute = next
        }
        publishProgressState()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
        stopProgressTicker()
        _playbackState.value = PlaybackState()
    }

    override fun release() {
        stopProgressTicker()
        exoPlayer.release()
    }

    private fun publishProgressState() {
        _playbackState.value = _playbackState.value.copy(
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.takeIf { it > 0 } ?: 0L,
            isPlaying = exoPlayer.isPlaying,
            volume = exoPlayer.volume,
            isMuted = exoPlayer.volume <= 0.001f
        )
    }

    private fun startProgressTicker() {
        progressHandler.removeCallbacks(progressTicker)
        progressHandler.post(progressTicker)
    }

    private fun stopProgressTicker() {
        progressHandler.removeCallbacks(progressTicker)
    }
}
