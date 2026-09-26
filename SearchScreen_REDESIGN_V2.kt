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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import com.sonify.music.presentation.SearchUiState
import com.sonify.music.presentation.theme.BeatBlue
import com.sonify.music.presentation.theme.BeatPink
import com.sonify.music.presentation.theme.BeatPurple

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onSongClick: (Int) -> Unit,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ========== HEADER WITH BACK BUTTON ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    "Find your next favorite",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
        }

        // ========== SEARCH INPUT FIELD ==========
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text("Search songs, artists, albums…", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp,
                                color = BeatPurple
                            )
                        }
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChanged("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = BeatPurple,
                    cursorColor = BeatPurple
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp)
            )
        }

        // ========== SEARCH CONTENT ==========
        when {
            // Error state with retry
            !state.error.isNullOrBlank() -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                        IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.Search,
                                "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Empty state when query is too short
            state.query.trim().length < 2 -> {
                SearchEmptyCard("Enter at least 2 characters to search")
            }

            // Loading state
            state.isLoading && state.results.isEmpty() -> {
                SearchLoading()
            }

            // No results found
            state.hasSearched && !state.isLoading && state.results.isEmpty() -> {
                SearchEmptyCard("No matches found. Try another search.")
            }

            // Results list
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        state.results,
                        key = { _, song -> song.videoId }
                    ) { index, song ->
                        SongRow(song, { onSongClick(index) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchLoading() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.5.dp,
            color = BeatPurple,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Searching…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SearchEmptyCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            BeatPurple.copy(0.08f),
                            BeatBlue.copy(0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Search,
                null,
                tint = BeatPurple,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
