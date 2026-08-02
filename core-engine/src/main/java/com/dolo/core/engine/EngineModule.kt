package com.dolo.core.engine

import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideYoutubeDL(): YoutubeDL = YoutubeDL

    @Provides
    @Singleton
    fun provideFFmpeg(): FFmpeg = FFmpeg

    @Provides
    @Singleton
    fun provideAria2c(): Aria2c = Aria2c
}
