package com.sonify.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String,
    val addedAt: Long = System.currentTimeMillis()
)

