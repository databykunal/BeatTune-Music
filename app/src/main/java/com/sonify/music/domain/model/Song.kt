package com.sonify.music.domain.model

data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String
)

