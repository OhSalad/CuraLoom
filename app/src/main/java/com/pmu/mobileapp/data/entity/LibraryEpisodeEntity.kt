package com.pmu.mobileapp.data.entity

import androidx.room.ColumnInfo
import com.pmu.mobileapp.model.LibraryEpisode

data class LibraryEpisodeEntity(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "feed_id")
    val feedId: Long,
    @ColumnInfo(name = "feed_title")
    val feedTitle: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "duration")
    val duration: String?,
    @ColumnInfo(name = "pub_date")
    val pubDate: String?,
    @ColumnInfo(name = "audio_url")
    val audioUrl: String?,
    @ColumnInfo(name = "last_position_ms")
    val lastPositionMs: Long,
    @ColumnInfo(name = "is_played")
    val isPlayed: Boolean,
    @ColumnInfo(name = "is_new")
    val isNew: Boolean
)

fun LibraryEpisodeEntity.toLibraryEpisode(): LibraryEpisode = LibraryEpisode(
    id = id,
    feedId = feedId,
    feedTitle = feedTitle,
    title = title,
    duration = duration,
    pubDate = pubDate,
    audioUrl = audioUrl,
    lastPositionMs = lastPositionMs.coerceAtLeast(0L),
    isPlayed = isPlayed,
    isNew = isNew
)
