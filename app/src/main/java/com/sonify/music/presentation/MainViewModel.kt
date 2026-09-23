package com.sonify.music.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonify.music.data.local.FavoriteSongEntity
import com.sonify.music.data.local.PlaylistEntity
import com.sonify.music.data.local.PlaylistSongEntity
import com.sonify.music.data.local.RecentlyPlayedEntity
import com.sonify.music.data.local.SongDao
import com.sonify.music.data.remote.YouTubeMusicApi
import com.sonify.music.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val api: YouTubeMusicApi,
    private val songDao: SongDao
) : ViewModel() {
    private val _homeState = MutableStateFlow(HomeUiState(isLoading = true))
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    val favoriteSongs: StateFlow<List<Song>> = songDao.getFavoriteSongs()
        .map { list -> list.map(::favoriteEntityToSong) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = songDao.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = songDao.getRecentlyPlayed()
        .map { list -> list.map(::recentEntityToSong) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val favoriteMutex = Mutex()
    private var homeJob: Job? = null
    private var searchJob: Job? = null
    private var homeRequestId = 0L
    private var searchRequestId = 0L

    init { loadHome("Trending music") }

    fun loadHome(query: String) {
        val normalized = query.trim().ifBlank { "Trending music" }
        val requestId = ++homeRequestId
        homeJob?.cancel()
        homeJob = viewModelScope.launch {
            _homeState.value = _homeState.value.copy(category = normalized, isLoading = true, error = null)
            try {
                val songs = withContext(Dispatchers.IO) { api.searchSongs(normalized) }.take(HOME_RESULT_LIMIT)
                if (requestId != homeRequestId) return@launch
                _homeState.value = HomeUiState(category = normalized, songs = songs, isLoading = false)
            } catch (t: Throwable) {
                if (requestId != homeRequestId) return@launch
                _homeState.value = _homeState.value.copy(isLoading = false, error = t.message ?: "Could not load music.")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val cleanQuery = query.replace("\n", " ").take(MAX_QUERY_LENGTH)
        ++searchRequestId
        searchJob?.cancel()
        _searchState.value = SearchUiState(query = cleanQuery)
        if (cleanQuery.trim().length < MIN_QUERY_LENGTH) return
        val requestId = searchRequestId
        searchJob = viewModelScope.launch {
            delay(350)
            performSearch(cleanQuery.trim(), requestId)
        }
    }

    fun submitSearch() {
        val query = _searchState.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        ++searchRequestId
        val requestId = searchRequestId
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query, requestId) }
    }

    fun retrySearch() = submitSearch()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            favoriteMutex.withLock {
                if (songDao.isFavoriteOnce(song.videoId)) songDao.removeFavorite(song.videoId)
                else songDao.insertFavorite(song.toEntity())
            }
        }
    }

    fun recordRecentlyPlayed(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.recordRecentlyPlayed(song.toRecentEntity())
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        val clean = name.trim().take(60)
        if (clean.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = songDao.createPlaylist(PlaylistEntity(name = clean))
            onCreated(id)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { songDao.deletePlaylistSongs(id); songDao.deletePlaylist(id) }
    }

    fun playlistSongs(id: Long): StateFlow<List<Song>> = songDao.getPlaylistSongs(id)
        .map { list -> list.map { Song(it.videoId, it.title, it.artist, it.thumbnailUrl, it.durationText) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.addToPlaylist(PlaylistSongEntity(playlistId, song.videoId, song.title, song.artist, song.thumbnailUrl, song.durationText))
        }
    }

    fun removeFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(Dispatchers.IO) { songDao.removeFromPlaylist(playlistId, song.videoId) }
    }

    private suspend fun performSearch(query: String, requestId: Long) {
        if (requestId != searchRequestId) return
        _searchState.value = _searchState.value.copy(isLoading = true, error = null, hasSearched = true)
        try {
            val results = withContext(Dispatchers.IO) { api.searchSongs(query) }
            if (requestId != searchRequestId || _searchState.value.query.trim() != query) return
            _searchState.value = _searchState.value.copy(results = results, isLoading = false, hasSearched = true)
        } catch (t: Throwable) {
            if (requestId != searchRequestId) return
            _searchState.value = _searchState.value.copy(results = emptyList(), isLoading = false, error = t.message ?: "Search failed.", hasSearched = true)
        }
    }

    private fun favoriteEntityToSong(entity: FavoriteSongEntity) =
        Song(entity.videoId, entity.title, entity.artist, entity.thumbnailUrl, entity.durationText)

    private fun recentEntityToSong(entity: RecentlyPlayedEntity) =
        Song(entity.videoId, entity.title, entity.artist, entity.thumbnailUrl, entity.durationText)
    private fun Song.toEntity() = FavoriteSongEntity(videoId, title, artist, thumbnailUrl, durationText)
    private fun Song.toRecentEntity() = RecentlyPlayedEntity(videoId, title, artist, thumbnailUrl, durationText)

    companion object {
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_QUERY_LENGTH = 120
        private const val HOME_RESULT_LIMIT = 12
    }
}

data class HomeUiState(
    val category: String = "Trending music",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)
