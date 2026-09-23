package com.sonify.music.di

import android.app.Application
import androidx.room.Room
import com.sonify.music.data.local.MusicDatabase
import com.sonify.music.data.local.SongDao
import com.sonify.music.data.remote.YouTubeMusicApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(app: Application): MusicDatabase =
        Room.databaseBuilder(app, MusicDatabase::class.java, "beattune_music_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideSongDao(db: MusicDatabase): SongDao = db.songDao

    @Provides
    @Singleton
    fun provideYouTubeMusicApi(): YouTubeMusicApi = YouTubeMusicApi()

}
