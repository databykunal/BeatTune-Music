package com.sonify.music.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.sonify.music.domain.model.Song
import com.sonify.music.presentation.HomeUiState
import com.sonify.music.presentation.theme.BeatBlue
import com.sonify.music.presentation.theme.BeatPink
import com.sonify.music.presentation.theme.BeatPurple

@Composable
fun HomeScreen(
    state: HomeUiState,
    userName: String,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onSongClick: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    recentlyPlayed: List<Song> = emptyList(),
    favorites: List<Song> = emptyList(),
    onRecentSongClick: (Int) -> Unit = {},
    onFavoriteSongClick: (Int) -> Unit = {},
    onViewAllCategory: (String) -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val categories = listOf(
        Triple("Trending", "Trending music", Icons.Rounded.Whatshot),
        Triple("New Release", "New releases", Icons.Rounded.Star),
        Triple("Chill", "Chill", Icons.Rounded.SelfImprovement),
        Triple("Workout", "Workout", Icons.Rounded.FitnessCenter)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ========== HEADER WITH GREETING & LOGO ==========
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Good ${greeting()},",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        userName.ifBlank { "Music Lover" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }
                
                // BeatTune Logo
                Surface(
                    onClick = onSettings,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "♪",
                            fontSize = 28.sp,
                            color = BeatPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // More options
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ========== SEARCH BAR ==========
        item {
            Surface(
                onClick = onOpenSearch,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Search songs, artists, albums…",
                        modifier = Modifier.padding(start = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ========== FEATURED PLAYLIST BANNER ==========
        item {
            val bannerSong = state.songs.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF7B4FD8),
                                Color(0xFF5E82C9)
                            )
                        )
                    )
            ) {
                if (bannerSong != null) {
                    AsyncImage(
                        model = bannerSong.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.3f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Text(
                        "Good Vibes",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Only",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                    Text(
                        "Chill • Focus • Feel Good",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
                
                Surface(
                    onClick = { if (state.songs.isNotEmpty()) onSongClick(0) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    contentColor = BeatPurple,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(20.dp)
                        .size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        // ========== CATEGORIES SECTION ==========
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp
                    )
                    Surface(
                        onClick = { onViewAllCategory("all") },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        contentColor = BeatPurple
                    ) {
                        Text(
                            "See all",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    itemsIndexed(categories) { _, category ->
                        val selected = state.category.equals(category.second, ignoreCase = true)
                        Surface(
                            onClick = { onCategorySelected(category.second) },
                            color = if (selected) BeatPurple else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.size(width = 90.dp, height = 80.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    category.third,
                                    null,
                                    tint = if (selected) Color.White else BeatPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    category.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // ========== RECENTLY PLAYED ==========
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recently Played",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        Surface(
                            onClick = { onViewAllCategory("recent") },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            contentColor = BeatPurple
                        ) {
                            Text(
                                "See all",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        itemsIndexed(recentlyPlayed.take(8)) { index, song ->
                            HomeMiniCard(song) { onRecentSongClick(index) }
                        }
                    }
                }
            }
        }

        // ========== YOUR FAVORITES ==========
        if (favorites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        Surface(
                            onClick = { onViewAllCategory("favorites") },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            contentColor = BeatPurple
                        ) {
                            Text(
                                "See all",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        itemsIndexed(favorites.take(8)) { index, song ->
                            HomeMiniCard(song) { onFavoriteSongClick(index) }
                        }
                    }
                }
            }
        }

        // ========== FRESH PICKS HEADER ==========
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (state.isLoading) "Finding fresh music…" else "Fresh picks for you",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = BeatPurple
                    )
                } else {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, "Refresh", tint = BeatPurple)
                    }
                }
            }
        }

        // ========== ERROR STATE ==========
        if (!state.error.isNullOrBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                        IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Refresh,
                                "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ========== LOADING STATE ==========
        if (state.isLoading && state.songs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = BeatPurple
                        )
                        Text(
                            "Loading your music…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ========== SONGS LIST ==========
        itemsIndexed(state.songs, key = { _, song -> song.videoId }) { index, song ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SongRow(song, { onSongClick(index) })
            }
        }
    }
}

@Composable
private fun HomeMiniCard(song: Song, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(width = 150.dp, height = 180.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Morning ☀️"
    in 12..16 -> "Afternoon 🌤️"
    else -> "Evening 🌙"
}
