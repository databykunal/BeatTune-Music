package com.sonify.music.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sonify.music.presentation.SearchUiState
import com.sonify.music.presentation.theme.BeatBlue
import com.sonify.music.presentation.theme.BeatPink
import com.sonify.music.presentation.theme.BeatPurple

@Composable
fun SearchScreen(state: SearchUiState, onQueryChanged: (String) -> Unit, onSubmit: () -> Unit, onRetry: () -> Unit, onSongClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)) {
            Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Find your next favorite song", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Songs, artists or albums…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        if (state.query.isNotEmpty()) IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Rounded.Clear, "Clear") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() })
            )
        }
        when {
            !state.error.isNullOrBlank() -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text(state.error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer); IconButton(onClick = onRetry) { Icon(Icons.Rounded.Refresh, "Retry") } }
            }
            state.query.trim().length < 2 -> SearchEmptyCard("Search for a song, artist or album")
            state.isLoading && state.results.isEmpty() -> SearchLoading()
            state.hasSearched && !state.isLoading && state.results.isEmpty() -> SearchEmptyCard("No matches found. Try another search.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.results, key = { _, song -> song.videoId }) { index, song -> SongRow(song, { onSongClick(index) }) }
            }
        }
    }
}

@Composable private fun SearchLoading() { Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(strokeWidth = 2.5.dp); Spacer(Modifier.height(12.dp)); Text("Searching…", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun SearchEmptyCard(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Column(modifier = Modifier.background(Brush.linearGradient(listOf(BeatPurple.copy(.08f), BeatBlue.copy(.05f), Color.Transparent))).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
