package com.sonify.music.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonify.music.domain.model.Song

@Composable
fun PlaylistDetailScreen(
    name: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Song) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("${songs.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onPlayAll, enabled = songs.isNotEmpty()) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text("Play")
                }
            }
        }
        if (songs.isEmpty()) item { Text("This playlist is empty. Add songs from the player.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) }
        itemsIndexed(songs, key = { _, song -> song.videoId }) { index, song ->
            SongRow(song, onClick = { onSongClick(index) }, trailingContent = {
                IconButton(onClick = { onRemove(song) }) { Icon(Icons.Rounded.DeleteOutline, "Remove from playlist") }
            })
        }
    }
}
