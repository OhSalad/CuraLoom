package com.pmu.mobileapp.model

data class LibraryEpisode(
    val id: Long,
    val feedId: Long,
    val feedTitle: String,
    val title: String,
    val duration: String?,
    val pubDate: String?,
    val audioUrl: String?,
    val isPlayed: Boolean,
    val isDownloaded: Boolean,
    val isNew: Boolean
)
