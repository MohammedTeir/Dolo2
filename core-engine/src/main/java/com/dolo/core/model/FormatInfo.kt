package com.dolo.core.model

data class FormatInfo(
    val formatId: String,
    val ext: String,
    val resolution: String?,
    val fps: Int = 0,
    val fileSizeBytes: Long = 0L,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val isVideoOnly: Boolean = false,
    val isAudioOnly: Boolean = false,
    val formatNote: String? = null,
    val url: String? = null,
    val vbr: Float? = null,
    val abr: Float? = null
)
