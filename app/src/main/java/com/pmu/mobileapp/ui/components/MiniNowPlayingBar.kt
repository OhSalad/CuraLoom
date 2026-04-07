package com.pmu.mobileapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDecreaseVolume: () -> Unit
) {
    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val showPause = playback.isPlaying && playback.status == PlaybackStatus.PLAYING
    val hasDuration = playback.durationMs > 0L
    var sliderValue by remember(playback.positionMs, playback.durationMs) { mutableFloatStateOf(progress) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenLibrary)) {
                    Text(
                        text = stringResource(R.string.now_playing_title),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = playback.episodeTitle,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(onClick = onDecreaseVolume) {
                        Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = stringResource(R.string.volume_down))
                    }
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (playback.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = stringResource(R.string.toggle_mute)
                        )
                    }
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (showPause) stringResource(R.string.pause_episode) else stringResource(R.string.play_episode)
                        )
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
                valueRange = 0f..1f
            )
        }
    }
}
