package com.sonify.music.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonify.music.player.playback.PlaybackState
import com.sonify.music.player.playback.RepeatMode
import com.sonify.music.presentation.theme.BeatBlue
import com.sonify.music.presentation.theme.BeatPink
import com.sonify.music.presentation.theme.BeatPurple

@Composable
fun PlayerScreen(
    state: PlaybackState,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRetry: () -> Unit
) {
    val song = state.currentSong ?: return
    val parsedDuration = parseDuration(song.durationText)
    val duration = state.durationMs.takeIf { it > 0L } ?: parsedDuration
    val sliderMax = duration.coerceAtLeast(1L).toFloat()
    var isDragging by remember(song.videoId) { mutableStateOf(false) }
    var dragPosition by remember(song.videoId) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2547),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ========== TOP CONTROLS ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        contentDescription = "Close player",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "NOW PLAYING",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenQueue, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            "Queue",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) BeatPink else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========== ALBUM ARTWORK ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.isLoading) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(60.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp),
                            color = BeatPurple,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ========== SONG INFO ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========== PROGRESS SLIDER ==========
            val displayedPosition = if (isDragging) dragPosition.toLong() else state.positionMs
            Slider(
                value = displayedPosition.coerceIn(0L, sliderMax.toLong()).toFloat(),
                onValueChange = {
                    isDragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek(dragPosition.toLong())
                },
                valueRange = 0f..sliderMax,
                enabled = duration > 0L && !state.isLoading,
                colors = SliderDefaults.colors(
                    activeTrackColor = BeatPurple,
                    thumbColor = BeatPurple,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ========== TIME DISPLAY ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatPosition(displayedPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    formatPosition(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ========== MAIN CONTROLS ROW ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleEnabled) BeatPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = BeatPurple,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.Repeat else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) BeatPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========== SECONDARY CONTROLS ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onToggleAutoplay,
                    color = if (state.isAutoplayEnabled) BeatPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            if (state.isAutoplayEnabled) "Autoplay: ON" else "Autoplay: OFF",
                            color = if (state.isAutoplayEnabled) BeatPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    onClick = onAddToPlaylist,
                    color = BeatBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "Add to Playlist",
                            color = BeatBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ========== ERROR STATE ==========
            if (!state.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = onRetry,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Try again", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatPosition(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        "$hours:${(minutes % 60).toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun parseDuration(durationText: String): Long {
    val parts = durationText.split(":")
    return when (parts.size) {
        2 -> (parts[0].toLongOrNull()?.times(60_000L) ?: 0L) +
            (parts[1].toLongOrNull()?.times(1_000L) ?: 0L)
        3 -> (parts[0].toLongOrNull()?.times(3_600_000L) ?: 0L) +
            (parts[1].toLongOrNull()?.times(60_000L) ?: 0L) +
            (parts[2].toLongOrNull()?.times(1_000L) ?: 0L)
        else -> 0L
    }
}
