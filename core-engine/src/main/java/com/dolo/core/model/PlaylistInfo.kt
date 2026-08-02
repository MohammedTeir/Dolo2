package com.dolo.core.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlaylistInfo(
    val id: String?,
    val title: String?,
    @JsonProperty("entries")
    val entries: List<PlaylistEntry>? = emptyList(),
    @JsonProperty("webpage_url")
    val webpageUrl: String?,
    @JsonProperty("_type")
    val type: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlaylistEntry(
    val id: String?,
    val title: String?,
    val url: String?,
    val uploader: String?,
    val duration: Int? = 0,
    val thumbnail: String? = null
)
