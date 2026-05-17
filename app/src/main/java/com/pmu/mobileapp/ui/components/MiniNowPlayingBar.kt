package com.pmu.mobileapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pmu.mobileapp.R
import com.pmu.mobileapp.player.PlaybackState
import com.pmu.mobileapp.player.PlaybackStatus

@Composable
fun MiniNowPlayingBar(
    modifier: Modifier = Modifier,
    playback: PlaybackState,
    onOpenLibrary: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleMute: () -> Unit,
    onDecreaseVolume: () -> Unit,
    onIncreaseVolume: () -> Unit,
    onDismiss: () -> Unit
) {
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val showPause = playback.isPlaying && playback.status == PlaybackStatus.PLAYING
    val hasDuration = playback.durationMs > 0L
    var sliderValue by remember(playback.positionMs, playback.durationMs) { mutableFloatStateOf(progress) }
    val sliderPositionMs = if (hasDuration) {
        (sliderValue * playback.durationMs).toLong()
    } else {
        playback.positionMs
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenLibrary)) {
                    Text(
                        text = stringResource(R.string.now_playing_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                    )
                    Text(
                        text = playback.episodeTitle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playback.feedTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (showPause) stringResource(R.string.pause_episode) else stringResource(R.string.play_episode)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dismiss_now_playing))
                    }
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    if (hasDuration) {
                        onSeekTo((sliderValue * playback.durationMs).toLong())
                    }
                },
                enabled = hasDuration,
                valueRange = 0f..1f,
                modifier = Modifier.heightIn(min = 36.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    activeTickColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.28f),
                    disabledActiveTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.36f),
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.18f),
                    disabledThumbColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.48f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatPlaybackTime(sliderPositionMs)} / ${if (hasDuration) formatPlaybackTime(playback.durationMs) else "--:--"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                )
                Row {
                    IconButton(modifier = Modifier.size(36.dp), onClick = onDecreaseVolume) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = stringResource(R.string.volume_down),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(modifier = Modifier.size(36.dp), onClick = onToggleMute) {
                        Icon(
                            imageVector = if (playback.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = if (playback.isMuted) stringResource(R.string.unmute_audio) else stringResource(R.string.mute_audio),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(modifier = Modifier.size(36.dp), onClick = onIncreaseVolume) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.volume_up),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
