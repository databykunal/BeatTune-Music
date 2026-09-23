package com.sonify.music.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipedSearchResponse(
    val items: List<PipedSearchItem> = emptyList()
)

@Serializable
data class PipedSearchItem(
    val type: String? = null,
    val title: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val duration: Long? = null
)

@Serializable
data class PipedStreamResponse(
    val title: String? = null,
    val audioStreams: List<PipedAudioStream> = emptyList()
)

@Serializable
data class PipedAudioStream(
    val url: String? = null,
    val format: String? = null,
    val quality: String? = null,
    val bitrate: Long? = null,
    val mimeType: String? = null
)

@Serializable
data class PipedInstance(
    val name: String? = null,
    @SerialName("api_url") val apiUrl: String? = null,
    @SerialName("uptime_24h") val uptime24h: Double? = null,
    @SerialName("uptime_7d") val uptime7d: Double? = null,
    val cdn: Boolean = false,
    @SerialName("up_to_date") val upToDate: Boolean = true
)
