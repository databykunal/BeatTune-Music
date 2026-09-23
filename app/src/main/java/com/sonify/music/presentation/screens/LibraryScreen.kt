package com.sonify.music.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonify.music.data.local.PlaylistEntity
import com.sonify.music.domain.model.Song
import com.sonify.music.presentation.theme.BeatPurple
import com.sonify.music.presentation.theme.BeatPurple

@Composable
fun LibraryScreen(favorites: List<Song>, recent: List<Song>, playlists: List<PlaylistEntity>, onSongClick: (List<Song>, Int) -> Unit, onRemoveFavorite: (Song) -> Unit, onCreatePlaylist: () -> Unit, onOpenPlaylist: (Long) -> Unit, onDeletePlaylist: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Your Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp))
        Text("Music you want to keep close", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 14.dp))
        androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { LibraryCard("Playlists", "${playlists.size} playlists", Icons.Rounded.PlaylistPlay, BeatPurple, onCreatePlaylist) }
            item { LibraryCard("Favorites", "${favorites.size} saved songs", Icons.Rounded.Favorite, BeatPurple) { if (favorites.isNotEmpty()) onSongClick(favorites, 0) } }
            item { LibraryCard("Recently Played", "${recent.size} recent songs", Icons.Rounded.History, BeatPurple) { if (recent.isNotEmpty()) onSongClick(recent, 0) } }
            item { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Your Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClick = onCreatePlaylist) { Icon(Icons.Rounded.Add, "Create playlist") } } }
            if (playlists.isEmpty()) item { LibraryCard("Create your first playlist", "Build your own music collection", Icons.Rounded.Add, BeatPurple, onCreatePlaylist) }
            items(playlists, key = { it.playlistId }) { playlist ->
                Surface(onClick = { onOpenPlaylist(playlist.playlistId) }, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) { Icon(Icons.Rounded.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(14.dp)) }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Open playlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { onDeletePlaylist(playlist.playlistId) }) { Icon(Icons.Rounded.MoreVert, "Playlist options") }
                    }
                }
            }
            if (favorites.isNotEmpty()) {
                item { Text("Favorites", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(favorites.take(12), key = { "fav_${it.videoId}" }) { song -> SongRow(song, { onSongClick(favorites, favorites.indexOf(song)) }, trailingContent = { IconButton(onClick = { onRemoveFavorite(song) }) { Icon(Icons.Rounded.Favorite, "Remove favorite", tint = BeatPurple) } }) }
            }
        }
    }
}

@Composable private fun LibraryCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = .16f), modifier = Modifier.size(52.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.padding(14.dp)) }
            Column(modifier = Modifier.padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
