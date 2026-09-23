package com.sonify.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM favorite_songs ORDER BY addedAt DESC")
    fun getFavoriteSongs(): Flow<List<FavoriteSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE videoId = :videoId")
    suspend fun removeFavorite(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE videoId = :videoId)")
    suspend fun isFavoriteOnce(videoId: String): Boolean

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: Long)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getPlaylistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToPlaylist(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeFromPlaylist(playlistId: Long, videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND videoId = :videoId)")
    suspend fun isInPlaylist(playlistId: Long, videoId: String): Boolean

    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 20")
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecentlyPlayed(song: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played")
    suspend fun clearRecentlyPlayed()
}
