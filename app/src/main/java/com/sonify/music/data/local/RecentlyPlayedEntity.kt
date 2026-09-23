package com.sonify.music.data.local

import androidx.room.Entity

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @androidx.room.PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String,
    val playedAt: Long = System.currentTimeMillis()
)
