package com.sonify.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteSongEntity::class, PlaylistEntity::class, PlaylistSongEntity::class, RecentlyPlayedEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract val songDao: SongDao
}
