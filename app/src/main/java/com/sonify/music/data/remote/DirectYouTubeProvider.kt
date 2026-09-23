package com.sonify.music.data.remote

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkHttpRequest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Direct YouTube provider used as BeatTune's primary source.
 *
 * This avoids making playback depend on a community-hosted Piped instance.
 * Piped remains available as a fallback in YouTubeMusicApi.
 */
class DirectYouTubeProvider {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    init {
        ensureInitialized()
    }

    fun searchSongs(query: String): List<SongCandidate> {
        ensureInitialized()
        val service = ServiceList.YouTube

        val extractors = listOf(
            runCatching {
                service.getSearchExtractor(query, listOf("music_songs"), "relevance")
            }.getOrNull(),
            runCatching { service.getSearchExtractor(query) }.getOrNull()
        )

        for (extractor in extractors.filterNotNull()) {
            try {
                extractor.fetchPage()
                val items = extractor.initialPage.items
                    .asSequence()
                    .mapNotNull { it as? StreamInfoItemExtractor }
                    .mapNotNull { item ->
                        runCatching {
                            val url = item.url
                            val videoId = extractVideoId(url) ?: return@runCatching null
                            SongCandidate(
                                videoId = videoId,
                                title = item.name.ifBlank { "Unknown title" },
                                artist = item.uploaderName.ifBlank { "Unknown artist" },
                                thumbnailUrl = item.thumbnails.firstOrNull()?.url
                                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                durationSeconds = item.duration.coerceAtLeast(0L)
                            )
                        }.getOrNull()
                    }
                    .distinctBy { it.videoId }
                    .toList()

                if (items.isNotEmpty()) return items
            } catch (_: Throwable) {
                // Try the next extractor/fallback provider.
            }
        }

        return emptyList()
    }

    fun getAudioStreamUrl(videoId: String): String? {
        ensureInitialized()
        val url = "https://www.youtube.com/watch?v=$videoId"
        return runCatching {
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            extractor.audioStreams
                .asSequence()
                .filter { it.isUrl && !it.url.isNullOrBlank() }
                .sortedByDescending { it.bitrate }
                .mapNotNull { it.url }
                .firstOrNull()
        }.getOrNull()
    }

    private fun extractVideoId(rawUrl: String): String? {
        val match = Regex("(?:v=|youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})").find(rawUrl)
        return match?.groupValues?.getOrNull(1)
    }

    private fun ensureInitialized() {
        synchronized(INIT_LOCK) {
            if (!initialized) {
                NewPipe.init(object : Downloader() {
                    override fun execute(request: Request): Response {
                        val builder = OkHttpRequest.Builder().url(request.url())
                        builder.header("User-Agent", USER_AGENT)
                        request.headers().forEach { (name, values) ->
                            if (values.isNotEmpty()) builder.header(name, values.joinToString(","))
                        }

                        val body = request.dataToSend()
                        val okRequest = when (request.httpMethod().uppercase()) {
                            "POST" -> builder.post((body ?: ByteArray(0)).toRequestBody(null)).build()
                            "HEAD" -> builder.head().build()
                            else -> builder.get().build()
                        }

                        client.newCall(okRequest).execute().use { response ->
                            val headers = response.headers.toMultimap()
                            val text = response.body?.string().orEmpty()
                            return Response(
                                response.code,
                                response.message,
                                headers,
                                text,
                                response.request.url.toString()
                            )
                        }
                    }
                })
                initialized = true
            }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
        private val INIT_LOCK = Any()
        @Volatile private var initialized = false
    }
}

data class SongCandidate(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSeconds: Long
)
