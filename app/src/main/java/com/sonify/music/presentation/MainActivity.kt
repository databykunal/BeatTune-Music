package com.sonify.music.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonify.music.presentation.theme.BeatBlue
import com.sonify.music.presentation.theme.BeatPink
import com.sonify.music.presentation.theme.BeatPurple
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonify.music.domain.model.Song
import com.sonify.music.player.playback.MusicPlaybackController
import com.sonify.music.player.playback.MusicPlaybackService
import com.sonify.music.presentation.screens.HomeScreen
import com.sonify.music.presentation.screens.LibraryScreen
import com.sonify.music.presentation.screens.MiniPlayer
import com.sonify.music.presentation.screens.PlayerScreen
import com.sonify.music.presentation.screens.PlaylistDetailScreen
import com.sonify.music.presentation.screens.SearchScreen
import com.sonify.music.presentation.theme.BeatTuneTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var playbackController: MusicPlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeatTuneTheme {
                val homeState by viewModel.homeState.collectAsStateWithLifecycle()
                val searchState by viewModel.searchState.collectAsStateWithLifecycle()
                val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
                val playlists by viewModel.playlists.collectAsStateWithLifecycle()
                val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
                val playbackState by playbackController.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var userName by rememberSaveable { mutableStateOf(getPreferences(MODE_PRIVATE).getString(PREF_NAME, "").orEmpty()) }
                var showNameSetup by rememberSaveable { mutableStateOf(userName.isBlank()) }
                var showPlayer by rememberSaveable { mutableStateOf(intent.getBooleanExtra(MusicPlaybackService.EXTRA_OPEN_NOW_PLAYING, false)) }
                var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
                var showCreatePlaylist by remember { mutableStateOf(false) }
                var showAddToPlaylist by remember { mutableStateOf(false) }
                var showQueue by remember { mutableStateOf(false) }

                val selectedPlaylist = playlists.firstOrNull { it.playlistId == selectedPlaylistId }
                val playlistSongsState = if (selectedPlaylistId != null) {
                    remember(selectedPlaylistId) { viewModel.playlistSongs(selectedPlaylistId!!) }
                } else null
                val playlistSongs by if (playlistSongsState != null) {
                    playlistSongsState.collectAsStateWithLifecycle()
                } else {
                    remember { mutableStateOf(emptyList()) }
                }
                val currentSong = playbackState.currentSong
                val isCurrentFavorite = currentSong?.videoId?.let { id -> favorites.any { it.videoId == id } } == true

                LaunchedEffect(currentSong?.videoId) { currentSong?.let(viewModel::recordRecentlyPlayed) }
                LaunchedEffect(playbackState.error, showPlayer) {
                    val error = playbackState.error ?: return@LaunchedEffect
                    if (!showPlayer) {
                        snackbarHostState.showSnackbar(error)
                        playbackController.clearError()
                    }
                }

                BackHandler(enabled = showPlayer || selectedPlaylistId != null || showNameSetup) {
                    when {
                        showPlayer -> showPlayer = false
                        selectedPlaylistId != null -> selectedPlaylistId = null
                    }
                }

                when {
                    showNameSetup -> NameSetupScreen(
                        onContinue = { name ->
                            val clean = name.trim().take(40)
                            if (clean.isNotBlank()) {
                                getPreferences(MODE_PRIVATE).edit().putString(PREF_NAME, clean).apply()
                                userName = clean
                                showNameSetup = false
                            }
                        }
                    )
                    showPlayer && currentSong != null -> PlayerScreen(
                        state = playbackState,
                        isFavorite = isCurrentFavorite,
                        onClose = { showPlayer = false },
                        onPlayPause = playbackController::togglePlayPause,
                        onPrevious = playbackController::skipPrevious,
                        onNext = playbackController::skipNext,
                        onSeek = playbackController::seekTo,
                        onToggleFavorite = { viewModel.toggleFavorite(currentSong) },
                        onToggleRepeat = playbackController::toggleRepeat,
                        onToggleShuffle = playbackController::toggleShuffle,
                        onToggleAutoplay = playbackController::toggleAutoplay,
                        onOpenQueue = { showQueue = true },
                        onAddToPlaylist = { showAddToPlaylist = true },
                        onRetry = playbackController::retryCurrentTrack
                    )
                    selectedPlaylistId != null && selectedPlaylist != null -> PlaylistDetailScreen(
                        name = selectedPlaylist.name,
                        songs = playlistSongs,
                        onBack = { selectedPlaylistId = null },
                        onPlayAll = { if (playlistSongs.isNotEmpty()) { showPlayer = true; playbackController.play(playlistSongs.first(), playlistSongs) } },
                        onSongClick = { index -> playlistSongs.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, playlistSongs) } },
                        onRemove = { viewModel.removeFromPlaylist(selectedPlaylist.playlistId, it) }
                    )
                    else -> Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            Column {
                                currentSong?.let { song ->
                                    MiniPlayer(
                                        song = song,
                                        isPlaying = playbackState.isPlaying,
                                        isLoading = playbackState.isLoading,
                                        progress = playbackProgress(playbackState.positionMs, playbackState.durationMs),
                                        onOpen = { showPlayer = true },
                                        onPlayPause = playbackController::togglePlayPause
                                    )
                                }
                                NavigationBar {
                                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("⌂") }, label = { Text("Home") })
                                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("⌕") }, label = { Text("Search") })
                                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("♫") }, label = { Text("Library") })
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { padding ->
                        Column(modifier = Modifier.padding(padding)) {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    state = homeState,
                                    onCategorySelected = viewModel::loadHome,
                                    onRetry = { viewModel.loadHome(homeState.category) },
                                    onSongClick = { index -> homeState.songs.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, homeState.songs) } },
                                    userName = userName,
                                    onOpenSearch = { selectedTab = 1 },
                                    recentlyPlayed = recent,
                                    favorites = favorites,
                                    onRecentSongClick = { index -> recent.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, recent) } },
                                    onFavoriteSongClick = { index -> favorites.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, favorites) } }
                                )
                                1 -> SearchScreen(
                                    state = searchState,
                                    onQueryChanged = viewModel::onSearchQueryChanged,
                                    onSubmit = viewModel::submitSearch,
                                    onRetry = viewModel::retrySearch,
                                    onSongClick = { index -> searchState.results.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, searchState.results) } }
                                )
                                else -> LibraryScreen(
                                    favorites = favorites,
                                    recent = recent,
                                    playlists = playlists,
                                    onSongClick = { songs, index -> songs.getOrNull(index)?.let { showPlayer = true; playbackController.play(it, songs) } },
                                    onRemoveFavorite = viewModel::toggleFavorite,
                                    onCreatePlaylist = { showCreatePlaylist = true },
                                    onOpenPlaylist = { selectedPlaylistId = it },
                                    onDeletePlaylist = viewModel::deletePlaylist
                                )
                            }
                        }
                    }
                }

                if (showCreatePlaylist) CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylist = false },
                    onCreate = { name -> viewModel.createPlaylist(name); showCreatePlaylist = false }
                )
                if (showAddToPlaylist && currentSong != null) AddToPlaylistDialog(
                    playlists = playlists,
                    song = currentSong,
                    onDismiss = { showAddToPlaylist = false },
                    onCreate = { name -> viewModel.createPlaylist(name) { viewModel.addToPlaylist(it, currentSong) }; showAddToPlaylist = false },
                    onAdd = { id -> viewModel.addToPlaylist(id, currentSong); showAddToPlaylist = false }
                )
                if (showQueue) QueueDialog(
                    queue = playbackState.queue,
                    currentIndex = playbackState.queueIndex,
                    onDismiss = { showQueue = false },
                    onSongClick = { song -> showQueue = false; playbackController.play(song, playbackState.queue) }
                )
            }
        }
    }

    companion object {
        private const val PREF_NAME = "beattune_display_name"
    }
}

@androidx.compose.runtime.Composable
private fun NameSetupScreen(onContinue: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF11172A), Color(0xFF070B18)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFF131B30), modifier = Modifier.size(92.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("B", style = MaterialTheme.typography.displaySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, color = BeatPurple)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Hey There! 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("What should we call you?", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("This will be your name in the app.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFAEB5C8), modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                placeholder = { Text("Enter your name…") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = { onContinue(name) }, enabled = name.trim().isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Continue  →", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New playlist") }, text = {
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Playlist name") })
    }, confirmButton = { Button(onClick = { onCreate(name) }, enabled = name.trim().isNotEmpty()) { Text("Create") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@androidx.compose.runtime.Composable
private fun AddToPlaylistDialog(
    playlists: List<com.sonify.music.data.local.PlaylistEntity>,
    song: Song,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onAdd: (Long) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to playlist") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(song.title, maxLines = 1)
            if (creating) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("New playlist") }, singleLine = true)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null)
                    TextButton(onClick = { creating = true }) { Text("Create new playlist") }
                }
                playlists.forEach { playlist -> TextButton(onClick = { onAdd(playlist.playlistId) }, modifier = Modifier.fillMaxWidth()) { Text(playlist.name) } }
            }
        }
    }, confirmButton = {
        if (creating) Button(onClick = { onCreate(name) }, enabled = name.trim().isNotEmpty()) { Text("Create & add") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@androidx.compose.runtime.Composable
private fun QueueDialog(queue: List<Song>, currentIndex: Int, onDismiss: () -> Unit, onSongClick: (Song) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.QueueMusic, null); Text("  Queue") } }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(queue) { index, song ->
                TextButton(onClick = { onSongClick(song) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (index == currentIndex) "▶  ${song.title}" else "${index + 1}. ${song.title}", maxLines = 1)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

private fun playbackProgress(positionMs: Long, durationMs: Long): Float = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
