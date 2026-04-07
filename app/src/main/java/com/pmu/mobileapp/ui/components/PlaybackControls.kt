package com.pmu.mobileapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pmu.mobileapp.R
import com.pmu.mobileapp.player.PlaybackStatus

@Composable
fun PlaybackControls(
    episodeTitle: String,
    feedTitle: String,
    isPlaying: Boolean,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit
) {
    val hasDuration = durationMs > 0L
    val progressFraction = if (hasDuration) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    var sliderValue by remember(positionMs, durationMs) { mutableFloatStateOf(progressFraction) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.now_playing_title),
                style = MaterialTheme.typography.labelMedium
            )
            Text(episodeTitle, fontWeight = FontWeight.SemiBold)
            Text(feedTitle, style = MaterialTheme.typography.bodySmall)

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    if (hasDuration) {
                        val target = (sliderValue * durationMs).toLong()
                        onSeekTo(target)
                    }
                },
                valueRange = 0f..1f,
                enabled = hasDuration
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(durationMs), style = MaterialTheme.typography.labelSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onSeekBack) {
                    Icon(Icons.Default.Replay10, contentDescription = stringResource(R.string.seek_back_10))
                }
                IconButton(onClick = onTogglePlayPause) {
                    val showPause = isPlaying && status == PlaybackStatus.PLAYING
                    Icon(
                        if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (showPause) stringResource(R.string.pause_episode) else stringResource(R.string.play_episode)
                    )
                }
                IconButton(onClick = onSeekForward) {
                    Icon(Icons.Default.Forward10, contentDescription = stringResource(R.string.seek_forward_10))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
