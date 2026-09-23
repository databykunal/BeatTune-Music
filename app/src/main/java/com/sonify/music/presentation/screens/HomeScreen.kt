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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Star
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
    onFavoriteSongClick: (Int) -> Unit = {}
) {
    val categories = listOf(
        Triple("Trending", "Trending music", Icons.Rounded.Whatshot),
        Triple("New", "New releases", Icons.Rounded.Star),
        Triple("Chill", "Chill", Icons.Rounded.SelfImprovement),
        Triple("Workout", "Workout", Icons.Rounded.FitnessCenter)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Good ${greeting()},", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Surface(onClick = onOpenSearch, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Search songs, artists, albums…", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            val bannerSong = state.songs.firstOrNull()
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF20284D), Color(0xFF31305E), Color(0xFF243A5A))))) {
                if (bannerSong != null) {
                    AsyncImage(model = bannerSong.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, alpha = 0.42f, modifier = Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)) {
                    Text("Good Vibes", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Only", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Chill • Focus • Feel Good", color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall)
                }
                Surface(onClick = { if (state.songs.isNotEmpty()) onSongClick(0) }, shape = androidx.compose.foundation.shape.CircleShape, color = Color.White, contentColor = BeatPurple, modifier = Modifier.align(Alignment.CenterEnd).padding(18.dp).size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null) }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("See all", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                itemsIndexed(categories) { _, category ->
                    val selected = state.category.equals(category.second, ignoreCase = true)
                    Surface(onClick = { onCategorySelected(category.second) }, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(width = 88.dp, height = 78.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(category.third, null, tint = if (selected) Color.White else MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(category.first, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (recentlyPlayed.isNotEmpty()) {
            item { SectionTitle("Recently Played") }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { itemsIndexed(recentlyPlayed.take(8)) { index, song -> HomeMiniCard(song, { onRecentSongClick(index) }) } } }
        }
        if (favorites.isNotEmpty()) {
            item { SectionTitle("Your Favorites") }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { itemsIndexed(favorites.take(8)) { index, song -> HomeMiniCard(song, { onFavoriteSongClick(index) }) } } }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.category, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (state.isLoading) "Finding fresh music…" else "Fresh picks for you", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) else IconButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, "Refresh") }
            }
        }
        if (!state.error.isNullOrBlank()) item { Text(state.error, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading && state.songs.isEmpty()) item { LoadingCard() }
        itemsIndexed(state.songs, key = { _, song -> song.videoId }) { index, song -> SongRow(song, { onSongClick(index) }) }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

@Composable private fun HomeMiniCard(song: Song, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(width = 150.dp, height = 178.dp)) {
        Column {
            AsyncImage(model = song.thumbnailUrl, contentDescription = song.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)))
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}

@Composable private fun LoadingCard() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp); Text("Loading your music…", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) { in 5..11 -> "Morning ☀️"; in 12..16 -> "Afternoon 🌤️"; else -> "Evening 🌙" }
