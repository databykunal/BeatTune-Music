package com.sonify.music.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sonify.music.data.remote.YouTubeMusicApi
import com.sonify.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val api: YouTubeMusicApi
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connection: CompletableDeferred<MediaController?>? = null
    private var positionJob: Job? = null
    private var playbackJob: Job? = null
    private var prefetchJob: Job? = null
    private var playbackGeneration = 0L
    private var lastRetriedVideoId: String? = null

    // This is the logical BeatTune queue. Media3's playlist contains the tracks whose
    // stream URLs have already been resolved. We grow that playlist around the
    // currently playing item so playback starts quickly without waiting for the
    // entire queue to resolve.
    private var queue: List<Song> = emptyList()
    private var queueIndex: Int = 0
    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var shuffleEnabled: Boolean = false
    private var autoplayEnabled: Boolean = true

    // Stream URLs can be fetched again after expiry/errors, so keep only a short-lived
    // in-memory cache. This makes next/previous and replay much faster during a session.
    private val streamCache = ConcurrentHashMap<String, CachedStream>()
    private val streamInFlight = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<String>>()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateFromPlayer()
            if (playbackState == Player.STATE_ENDED) {
                scope.launch { handleNaturalEnd() }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            lastRetriedVideoId = null
            val index = queue.indexOfFirst { it.videoId == id }
            if (index >= 0) {
                queueIndex = index
                _state.value = _state.value.copy(
                    currentSong = queue[index],
                    queue = queue,
                    queueIndex = index,
                    isLoading = controller?.playbackState == Player.STATE_BUFFERING,
                    isPlaying = controller?.isPlaying == true,
                    positionMs = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                    durationMs = controller?.duration?.takeIf { it > 0L } ?: 0L
                )
                schedulePrefetch(playbackGeneration, index)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val current = _state.value.currentSong ?: return
            if (lastRetriedVideoId == current.videoId) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    error = "This song could not be played right now. Try again or choose another track."
                )
                return
            }
            lastRetriedVideoId = current.videoId
            playbackJob?.cancel()
            playbackJob = scope.launch {
                val generation = playbackGeneration
                _state.value = _state.value.copy(
                    isLoading = true,
                    isPlaying = false,
                    error = "Refreshing the audio source…"
                )
                val freshUrl = withTimeoutOrNull(15_000L) {
                    resolveStreamUrl(current, forceRefresh = true)
                }
                if (generation != playbackGeneration) return@launch
                if (freshUrl.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = "This song could not be played right now. Try again or choose another track."
                    )
                    return@launch
                }

                val mediaController = getController() ?: return@launch
                val index = mediaController.currentMediaItemIndex
                val item = buildMediaItem(current, freshUrl)
                if (index >= 0 && index < mediaController.mediaItemCount) {
                    mediaController.replaceMediaItem(index, item)
                } else {
                    mediaController.setMediaItem(item)
                }
                mediaController.prepare()
                mediaController.play()
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = true,
                    error = null
                )
                schedulePrefetch(generation, queueIndex)
            }
        }
    }

    init {
        connect()
    }

    fun play(song: Song, queue: List<Song> = listOf(song)) {
        val normalizedQueue = queue
            .asSequence()
            .plus(sequenceOf(song))
            .distinctBy(Song::videoId)
            .toList()
            .ifEmpty { listOf(song) }

        this.queue = normalizedQueue
        this.queueIndex = normalizedQueue.indexOfFirst { it.videoId == song.videoId }.coerceAtLeast(0)
        playbackGeneration++
        val generation = playbackGeneration
        lastRetriedVideoId = null

        playbackJob?.cancel()
        prefetchJob?.cancel()
        playbackJob = scope.launch {
            startCurrent(generation)
        }
    }

    fun togglePlayPause() {
        val mediaController = controller
        if (mediaController == null) {
            val song = _state.value.currentSong ?: return
            playbackJob?.cancel()
            playbackJob = scope.launch { startCurrent(playbackGeneration, song) }
            return
        }

        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
        updateFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        controller?.let { mediaController ->
            val max = mediaController.duration
            val target = if (max > 0L) positionMs.coerceIn(0L, max) else positionMs.coerceAtLeast(0L)
            mediaController.seekTo(target)
        }
        updateFromPlayer()
    }

    fun skipNext() {
        val mediaController = controller ?: return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val nextIndex = when {
                shuffleEnabled -> nextLogicalRandomIndex()
                queueIndex + 1 < queue.size -> queueIndex + 1
                repeatMode == RepeatMode.ALL -> 0
                else -> -1
            }
            if (nextIndex !in queue.indices) return@launch

            // Always follow the logical BeatTune queue instead of relying on
            // Media3's current next item. Prefetch requests finish concurrently,
            // so completion order must never decide what "next" means.
            ensureQueueItemLoaded(nextIndex, insertAtFront = false, generation = playbackGeneration)
            val target = findMediaItemIndex(queue[nextIndex].videoId)
            if (target >= 0) {
                mediaController.seekTo(target, 0L)
                mediaController.play()
            }
            updateFromPlayer()
        }
    }

    fun skipPrevious() {
        val mediaController = controller ?: return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val currentPosition = mediaController.currentPosition
            if (currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
                mediaController.seekTo(0L)
                updateFromPlayer()
                return@launch
            }

            val previousIndex = if (queueIndex > 0) queueIndex - 1 else {
                if (repeatMode == RepeatMode.ALL) queue.lastIndex else -1
            }
            if (previousIndex !in queue.indices) {
                mediaController.seekTo(0L)
                return@launch
            }

            // Resolve and target the logical previous song directly. This remains
            // correct even when another prefetch completed before this one.
            ensureQueueItemLoaded(previousIndex, insertAtFront = true, generation = playbackGeneration)
            val target = findMediaItemIndex(queue[previousIndex].videoId)
            if (target >= 0) {
                mediaController.seekTo(target, 0L)
                mediaController.play()
            }
            updateFromPlayer()
        }
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = repeatMode)

        // We handle queue-wide repeat ourselves because the logical BeatTune queue can
        // be larger than the currently-resolved Media3 playlist.
        controller?.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun toggleAutoplay() {
        autoplayEnabled = !autoplayEnabled
        _state.value = _state.value.copy(isAutoplayEnabled = autoplayEnabled)
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        controller?.shuffleModeEnabled = shuffleEnabled
        _state.value = _state.value.copy(isShuffleEnabled = shuffleEnabled)
    }

    fun retryCurrentTrack() {
        val song = _state.value.currentSong ?: return
        playbackJob?.cancel()
        streamCache.remove(song.videoId)
        playbackGeneration++
        val generation = playbackGeneration
        lastRetriedVideoId = null
        playbackJob = scope.launch { startCurrent(generation, song, forceRefresh = true) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private suspend fun startCurrent(
        generation: Long,
        explicitSong: Song? = null,
        forceRefresh: Boolean = false
    ) {
        val mediaController = getController() ?: run {
            _state.value = _state.value.copy(
                isLoading = false,
                isPlaying = false,
                error = "The playback service could not be reached. Please reopen BeatTune."
            )
            return
        }

        val song = explicitSong ?: queue.getOrNull(queueIndex) ?: return
        queueIndex = queue.indexOfFirst { it.videoId == song.videoId }.coerceAtLeast(0)

        _state.value = PlaybackState(
            currentSong = song,
            queue = queue,
            queueIndex = queueIndex,
            isLoading = true,
            isPlaying = false,
            repeatMode = repeatMode,
            isShuffleEnabled = shuffleEnabled,
            isAutoplayEnabled = autoplayEnabled
        )

        try {
            // Start resolving the adjacent tracks at the same time as the current
            // track. The current song is allowed to start as soon as its URL is ready.
            val currentDeferred = scope.async(Dispatchers.IO) {
                resolveStreamUrl(song, forceRefresh)
            }
            schedulePrefetch(generation, queueIndex)

            val streamUrl = currentDeferred.await()
            if (generation != playbackGeneration) return

            mediaController.setMediaItem(buildMediaItem(song, streamUrl))
            mediaController.repeatMode = Player.REPEAT_MODE_OFF
            mediaController.shuffleModeEnabled = shuffleEnabled
            mediaController.prepare()
            mediaController.play()

            _state.value = _state.value.copy(
                isLoading = false,
                isPlaying = true,
                positionMs = 0L,
                durationMs = 0L,
                error = null
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            if (generation != playbackGeneration) return
            _state.value = _state.value.copy(
                isLoading = false,
                isPlaying = false,
                error = t.message ?: "Could not play this track."
            )
        }
    }

    private fun schedulePrefetch(
        generation: Long,
        centerIndex: Int
    ) {
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val targetIndexes = buildList {
                if (centerIndex > 0) add(centerIndex - 1)
                if (centerIndex + 1 < queue.size) add(centerIndex + 1)
                if (centerIndex + 2 < queue.size) add(centerIndex + 2)
            }.distinct()

            if (targetIndexes.isEmpty()) return@launch

            // Resolve neighbours in parallel. This is the main continuity optimisation:
            // the next track is fetched while the current one is playing.
            targetIndexes
                .map { index ->
                    async(Dispatchers.IO) {
                        if (generation != playbackGeneration) return@async index to null
                        val cached = resolveStreamUrl(queue[index])
                        index to cached
                    }
                }
                .awaitAll()
                .forEach { (index, url) ->
                    if (generation == playbackGeneration && !url.isNullOrBlank()) {
                        addResolvedItem(index, url)
                    }
                }
        }
    }

    private suspend fun ensureQueueItemLoaded(
        index: Int,
        insertAtFront: Boolean,
        generation: Long
    ) {
        if (index !in queue.indices || generation != playbackGeneration) return
        val song = queue[index]
        if (findMediaItemIndex(song.videoId) >= 0) return

        val url = resolveStreamUrl(song)
        if (generation != playbackGeneration) return
        addResolvedItem(index, url, insertAtFront)
    }

    private fun addResolvedItem(index: Int, url: String, forceFront: Boolean = false) {
        val mediaController = controller ?: return
        if (index !in queue.indices) return
        val song = queue[index]
        if (findMediaItemIndex(song.videoId) >= 0) return

        val mediaItem = buildMediaItem(song, url)

        // Insert according to the logical queue position, not according to the
        // order in which network requests finish. This is important because the
        // previous/next/next+2 streams are resolved concurrently.
        val insertIndex = if (forceFront) {
            0
        } else {
            var position = 0
            for (mediaPosition in 0 until mediaController.mediaItemCount) {
                val existingId = mediaController.getMediaItemAt(mediaPosition).mediaId
                val existingQueueIndex = queue.indexOfFirst { it.videoId == existingId }
                if (existingQueueIndex >= 0 && existingQueueIndex < index) {
                    position++
                }
            }
            position.coerceIn(0, mediaController.mediaItemCount)
        }

        mediaController.addMediaItem(insertIndex, mediaItem)
    }

    private suspend fun handleNaturalEnd() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) return
        if (queue.isEmpty() || !autoplayEnabled) return

        val nextIndex = when {
            repeatMode == RepeatMode.ONE -> queueIndex
            shuffleEnabled -> nextLogicalRandomIndex()
            queueIndex + 1 < queue.size -> queueIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> -1
        }

        if (nextIndex !in queue.indices) return

        ensureQueueItemLoaded(nextIndex, insertAtFront = false, generation = playbackGeneration)
        if (nextIndex == queueIndex && repeatMode == RepeatMode.ONE) {
            val target = findMediaItemIndex(queueIndexSongId() ?: return)
            if (target >= 0) {
                mediaController.seekTo(target, 0L)
                mediaController.play()
            }
            return
        }

        val target = findMediaItemIndex(queue[nextIndex].videoId)
        if (target >= 0) {
            mediaController.seekTo(target, 0L)
            mediaController.play()
        }
    }

    private suspend fun resolveStreamUrl(song: Song, forceRefresh: Boolean = false): String {
        if (!forceRefresh) {
            val cached = streamCache[song.videoId]
            if (cached != null && System.currentTimeMillis() - cached.createdAt < STREAM_CACHE_TTL_MS) {
                return cached.url
            }
        }

        if (forceRefresh) streamCache.remove(song.videoId)
        streamInFlight[song.videoId]?.let { return it.await() }

        val deferred = scope.async(Dispatchers.IO) {
            api.getAudioStreamUrl(song.videoId)
        }
        val existing = streamInFlight.putIfAbsent(song.videoId, deferred)
        if (existing != null) {
            return existing.await()
        }

        return try {
            val result = deferred.await()
            streamCache[song.videoId] = CachedStream(result, System.currentTimeMillis())
            result
        } finally {
            streamInFlight.remove(song.videoId, deferred)
        }
    }

    private fun buildMediaItem(song: Song, streamUrl: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.videoId)
            .setUri(Uri.parse(streamUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle("BeatTune")
                    .setArtworkUri(Uri.parse(song.thumbnailUrl))
                    .build()
            )
            .build()

    private fun findMediaItemIndex(videoId: String): Int {
        val mediaController = controller ?: return -1
        for (index in 0 until mediaController.mediaItemCount) {
            if (mediaController.getMediaItemAt(index).mediaId == videoId) return index
        }
        return -1
    }

    private fun queueIndexSongId(): String? = queue.getOrNull(queueIndex)?.videoId

    private fun nextLogicalRandomIndex(): Int {
        if (queue.size <= 1) return queueIndex
        return queue.indices.filter { it != queueIndex }.random()
    }

    private fun updateFromPlayer() {
        val mediaController = controller ?: return
        val mediaId = mediaController.currentMediaItem?.mediaId
        val mediaSong = queue.firstOrNull { it.videoId == mediaId }
        val currentSong = mediaSong ?: _state.value.currentSong ?: mediaController.currentMediaItem?.let(::songFromMediaItem)
        val index = mediaId?.let { id -> queue.indexOfFirst { it.videoId == id } }
            ?.takeIf { it >= 0 }
            ?: queueIndex

        if (mediaSong != null) queueIndex = index

        _state.value = _state.value.copy(
            currentSong = currentSong,
            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
            durationMs = mediaController.duration.takeIf { it > 0L } ?: 0L,
            isPlaying = mediaController.isPlaying,
            isLoading = mediaController.playbackState == Player.STATE_BUFFERING,
            queue = queue,
            queueIndex = queueIndex
        )
    }

    private fun songFromMediaItem(item: MediaItem): Song = Song(
        videoId = item.mediaId,
        title = item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Unknown title" },
        artist = item.mediaMetadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" },
        thumbnailUrl = item.mediaMetadata.artworkUri?.toString().orEmpty(),
        durationText = "--:--"
    )

    private fun connect(): CompletableDeferred<MediaController?> {
        connection?.let { return it }

        val deferred = CompletableDeferred<MediaController?>()
        connection = deferred
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java)
        )

        val future: ListenableFuture<MediaController> = MediaController.Builder(appContext, sessionToken)
            .buildAsync()

        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    deferred.complete(mediaController)
                    startPositionPolling()
                    updateFromPlayer()
                } catch (t: Throwable) {
                    if (connection === deferred) connection = null
                    deferred.complete(null)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )

        return deferred
    }

    private suspend fun getController(): MediaController? {
        val active = controller
        if (active != null) return active
        return connect().await()
    }

    private fun startPositionPolling() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                updateFromPlayer()
                delay(POSITION_POLL_MS)
            }
        }
    }

    private data class CachedStream(
        val url: String,
        val createdAt: Long
    )

    companion object {
        private const val POSITION_POLL_MS = 250L
        private const val PREVIOUS_RESTART_THRESHOLD_MS = 5_000L
        private const val STREAM_CACHE_TTL_MS = 3 * 60 * 1_000L
    }
}

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val isAutoplayEnabled: Boolean = true,
    val error: String? = null
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}
