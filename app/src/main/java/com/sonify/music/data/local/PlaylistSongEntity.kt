package com.sonify.music.data.local

import androidx.room.Entity

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "videoId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String,
    val addedAt: Long = System.currentTimeMillis()
)
