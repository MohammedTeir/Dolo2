package com.dolo.core.engine

import android.content.Context
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class EngineInitializer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val youtubeDL: YoutubeDL,
    private val ffmpeg: FFmpeg,
    private val aria2c: Aria2c
) {
    private val _initState = MutableStateFlow<EngineInitState>(EngineInitState.Idle)
    val initState: StateFlow<EngineInitState> = _initState.asStateFlow()

    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) {
            _initState.value = EngineInitState.Success
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _initState.value = EngineInitState.Initializing("Initializing yt-dlp runtime...")
                youtubeDL.init(context)

                _initState.value = EngineInitState.Initializing("Initializing FFmpeg...")
                ffmpeg.init(context)

                _initState.value = EngineInitState.Initializing("Initializing aria2c...")
                aria2c.init(context)

                isInitialized = true
                _initState.value = EngineInitState.Success
            } catch (e: Exception) {
                Log.e("EngineInitializer", "Failed to initialize engine", e)
                val causeMsg = e.cause?.message
                val mainMsg = e.localizedMessage ?: "Unknown initialization error"
                val detailedError = if (!causeMsg.isNullOrBlank() && causeMsg != mainMsg) {
                    "$mainMsg ($causeMsg)"
                } else {
                    mainMsg
                }
                _initState.value = EngineInitState.Error(detailedError)
            }
        }
    }
}
