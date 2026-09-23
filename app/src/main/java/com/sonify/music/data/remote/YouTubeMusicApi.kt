package com.sonify.music.data.remote

import android.net.Uri
import android.util.Log
import com.sonify.music.domain.model.Song
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Thin network gateway for public Piped instances.
 *
 * Piped is community-hosted, so a single hard-coded host is not reliable enough
 * for a real app. We discover healthy instances, keep a cached list, remember the
 * last successful host, and keep a small fallback list for registry failures.
 */
class YouTubeMusicApi {
    private val directProvider = DirectYouTubeProvider()

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    private val instanceMutex = Mutex()
    private var cachedInstances: List<String> = FALLBACK_INSTANCES
    private var lastInstanceRefresh = System.currentTimeMillis()
    private var lastSuccessfulInstance: String? = null
    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, CachedStream>()
    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, CachedSearch>()

    suspend fun searchSongs(query: String): List<Song> {
        val normalizedQuery = query.trim()
        require(normalizedQuery.isNotEmpty()) { "Search query cannot be empty" }

        searchCache[normalizedQuery]?.let { cached ->
            if (System.currentTimeMillis() - cached.createdAt < SEARCH_CACHE_TTL_MS) {
                return cached.songs
            }
            searchCache.remove(normalizedQuery)
        }

        // Run the direct extractor and Piped fallback in parallel. The first non-empty
        // result wins, so a slow/unhealthy provider does not unnecessarily block search.
        val result = coroutineScope {
            val direct = async(Dispatchers.IO) {
                runCatching {
                    withTimeout(DIRECT_SEARCH_TIMEOUT_MS) {
                        directProvider.searchSongs(normalizedQuery).map { candidate ->
                            Song(
                                videoId = candidate.videoId,
                                title = candidate.title,
                                artist = candidate.artist,
                                thumbnailUrl = candidate.thumbnailUrl,
                                durationText = formatDuration(candidate.durationSeconds)
                            )
                        }
                    }
                }.getOrElse {
                    Log.w(TAG, "Direct YouTube search failed", it)
                    emptyList()
                }
            }
            val fallback = async(Dispatchers.IO) {
                runCatching { searchViaPiped(normalizedQuery) }
                    .getOrElse {
                        Log.w(TAG, "Piped search failed", it)
                        emptyList()
                    }
            }

            val first = select<List<Song>?> {
                direct.onAwait { if (it.isNotEmpty()) it else fallback.await().takeIf { it.isNotEmpty() } }
                fallback.onAwait { if (it.isNotEmpty()) it else direct.await().takeIf { it.isNotEmpty() } }
            }
            direct.cancel()
            fallback.cancel()
            first.orEmpty()
        }

        if (result.isEmpty()) {
            throw ApiException(
                "Music search is temporarily unavailable. Please check your connection and try again."
            )
        }

        searchCache[normalizedQuery] = CachedSearch(result, System.currentTimeMillis())
        return result
    }

    private suspend fun searchViaPiped(query: String): List<Song> {
        var candidates = getInstances()
        var lastError: Throwable? = null

        repeat(MAX_SEARCH_PASSES) { pass ->
            for (instance in prioritizeLastSuccessful(candidates).take(MAX_FALLBACK_INSTANCES)) {
                try {
                    val response = client.get("$instance/search") {
                        parameter("q", query)
                        parameter("filter", "music_songs")
                    }.body<PipedSearchResponse>()

                    val songs = response.items
                        .asSequence()
                        .mapNotNull(::toSong)
                        .distinctBy(Song::videoId)
                        .toList()

                    markSuccessful(instance)
                    if (songs.isNotEmpty()) return songs
                } catch (t: Throwable) {
                    lastError = t
                    Log.w(TAG, "Search failed on $instance", t)
                }
            }

            if (pass == 0) candidates = getInstances(forceRefresh = true)
        }

        throw ApiException("Piped search failed", lastError)
    }

    suspend fun getAudioStreamUrl(videoId: String): String {
        require(videoId.isNotBlank()) { "Video id cannot be empty" }

        val cached = streamCache[videoId]
        if (cached != null && System.currentTimeMillis() - cached.createdAt < STREAM_CACHE_TTL_MS) {
            return cached.url
        }
        streamCache.remove(videoId)

        // Race the direct extractor and the Piped fallback. This reduces the usual
        // "wait for primary provider to time out, then try fallback" delay.
        val result = coroutineScope {
            val direct = async(Dispatchers.IO) {
                runCatching {
                    withTimeout(DIRECT_STREAM_TIMEOUT_MS) { directProvider.getAudioStreamUrl(videoId) }
                }.getOrNull()
            }
            val fallback = async(Dispatchers.IO) {
                runCatching { getPipedAudioStreamUrl(videoId) }.getOrNull()
            }

            val first = select<String?> {
                direct.onAwait { url -> if (!url.isNullOrBlank()) url else fallback.await() }
                fallback.onAwait { url -> if (!url.isNullOrBlank()) url else direct.await() }
            }
            direct.cancel()
            fallback.cancel()
            first
        }

        val url = result?.takeIf { it.isNotBlank() }
            ?: throw ApiException(
                "This song could not be played right now. Try again or choose another track."
            )

        streamCache[videoId] = CachedStream(url, System.currentTimeMillis())
        return url
    }

    private suspend fun getPipedAudioStreamUrl(videoId: String): String? {
        var candidates = getInstances()
        var lastError: Throwable? = null

        repeat(MAX_STREAM_PASSES) { pass ->
            for (instance in prioritizeLastSuccessful(candidates).take(MAX_FALLBACK_INSTANCES)) {
                try {
                    val response = client.get("$instance/streams/${Uri.encode(videoId)}")
                        .body<PipedStreamResponse>()

                    val stream = response.audioStreams
                        .asSequence()
                        .filter { !it.url.isNullOrBlank() }
                        .sortedByDescending { it.bitrate ?: 0L }
                        .firstOrNull { audio ->
                            audio.format?.equals("M4A", ignoreCase = true) == true ||
                                audio.mimeType?.startsWith("audio/mp4", ignoreCase = true) == true ||
                                audio.mimeType?.startsWith("audio/", ignoreCase = true) == true
                        }
                        ?: response.audioStreams
                            .asSequence()
                            .filter { !it.url.isNullOrBlank() }
                            .maxByOrNull { it.bitrate ?: 0L }

                    val playableUrl = stream?.url?.takeIf { it.isNotBlank() }
                    if (playableUrl != null) {
                        markSuccessful(instance)
                        return playableUrl
                    }
                    lastError = ApiException("The selected source returned no playable audio stream.")
                } catch (t: Throwable) {
                    lastError = t
                    Log.w(TAG, "Stream lookup failed on $instance", t)
                }
            }
            if (pass == 0) candidates = getInstances(forceRefresh = true)
        }

        throw ApiException("Piped playback failed", lastError)
    }

    private suspend fun getInstances(forceRefresh: Boolean = false): List<String> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastInstanceRefresh < INSTANCE_REFRESH_MS) {
            return cachedInstances
        }

        return instanceMutex.withLock {
            val refreshedNow = System.currentTimeMillis()
            if (!forceRefresh && refreshedNow - lastInstanceRefresh < INSTANCE_REFRESH_MS) {
                return@withLock cachedInstances
            }

            try {
                val discovered = client.get(INSTANCE_REGISTRY_URL)
                    .body<List<PipedInstance>>()
                    .asSequence()
                    .filter { it.apiUrl?.startsWith("https://", ignoreCase = true) == true }
                    .filter { (it.uptime24h ?: 100.0) >= MIN_INSTANCE_UPTIME }
                    .sortedWith(
                        compareByDescending<PipedInstance> { it.cdn }
                            .thenByDescending { it.uptime24h ?: 0.0 }
                            .thenByDescending { it.uptime7d ?: 0.0 }
                    )
                    .mapNotNull { it.apiUrl?.let(::normalizeBaseUrl) }
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(MAX_DYNAMIC_INSTANCES)
                    .toList()

                // Keep fallback hosts in the candidate pool instead of replacing them.
                // This protects the app when the public registry itself is stale.
                cachedInstances = (discovered + FALLBACK_INSTANCES)
                    .map(::normalizeBaseUrl)
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(MAX_DYNAMIC_INSTANCES + FALLBACK_INSTANCES.size)
            } catch (t: Throwable) {
                Log.w(TAG, "Could not refresh Piped instance registry", t)
                cachedInstances = (cachedInstances + FALLBACK_INSTANCES)
                    .map(::normalizeBaseUrl)
                    .filter(String::isNotBlank)
                    .distinct()
            }

            lastInstanceRefresh = refreshedNow
            cachedInstances
        }
    }

    private fun prioritizeLastSuccessful(instances: List<String>): List<String> {
        val last = lastSuccessfulInstance ?: return instances
        return listOf(last) + instances.filterNot { it == last }
    }

    private fun markSuccessful(instance: String) {
        lastSuccessfulInstance = instance
    }

    private fun toSong(item: PipedSearchItem): Song? {
        if (!item.type.equals("stream", ignoreCase = true)) return null

        val videoId = extractVideoId(item.url ?: return null) ?: return null
        val duration = (item.duration ?: 0L).coerceAtLeast(0L)
        // Use YouTube's thumbnail host directly instead of an instance-specific proxy.
        val thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Song(
            videoId = videoId,
            title = item.title?.ifBlank { "Unknown title" } ?: "Unknown title",
            artist = item.uploaderName?.takeIf { it.isNotBlank() } ?: "Unknown artist",
            thumbnailUrl = thumbnail,
            durationText = formatDuration(duration)
        )
    }

    private fun extractVideoId(rawUrl: String): String? {
        return try {
            val uri = if (rawUrl.startsWith("http", ignoreCase = true)) {
                Uri.parse(rawUrl)
            } else {
                Uri.parse("https://youtube.com$rawUrl")
            }

            uri.getQueryParameter("v")?.takeIf(::isValidVideoId)
                ?: uri.pathSegments.lastOrNull()?.takeIf(::isValidVideoId)
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidVideoId(value: String): Boolean =
        value.length == 11 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun normalizeBaseUrl(value: String): String = value.trim().trimEnd('/')

    private fun formatDuration(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "--:--"
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val hours = minutes / 60L
        return if (hours > 0L) {
            "$hours:${(minutes % 60L).toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }

    private data class CachedStream(
        val url: String,
        val createdAt: Long
    )

    private data class CachedSearch(
        val songs: List<Song>,
        val createdAt: Long
    )

    companion object {
        private const val TAG = "YouTubeMusicApi"
        private const val INSTANCE_REGISTRY_URL = "https://piped-instances.kavin.rocks/"
        private const val REQUEST_TIMEOUT_MS = 6_500L
        private const val CONNECT_TIMEOUT_MS = 2_500L
        private const val SOCKET_TIMEOUT_MS = 6_500L
        private const val INSTANCE_REFRESH_MS = 30 * 60 * 1_000L
        private const val MIN_INSTANCE_UPTIME = 70.0
        private const val MAX_DYNAMIC_INSTANCES = 6
        private const val MAX_SEARCH_PASSES = 1
        private const val MAX_STREAM_PASSES = 1
        private const val MAX_FALLBACK_INSTANCES = 2
        private const val DIRECT_SEARCH_TIMEOUT_MS = 8_000L
        private const val DIRECT_STREAM_TIMEOUT_MS = 7_000L
        private const val STREAM_CACHE_TTL_MS = 3 * 60 * 1_000L
        private const val SEARCH_CACHE_TTL_MS = 2 * 60 * 1_000L

        private val FALLBACK_INSTANCES = listOf(
            // Healthy in recent community instance checks; keep these as emergency fallbacks.
            "https://api.piped.private.coffee",
            "https://pipedapi.ducks.party",
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.nosebs.ru",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.syncpundit.io",
            "https://api-piped.mha.fi",
            "https://piped-api.garudalinux.org"
        )
    }
}

class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
