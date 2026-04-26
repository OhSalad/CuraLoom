package com.pmu.mobileapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pmu.mobileapp.model.Episode

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = FeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["feed_id"])]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "feed_id")
    val feedId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "pub_date")
    val pubDate: String? = null,
    @ColumnInfo(name = "duration")
    val duration: String? = null,
    @ColumnInfo(name = "audio_url")
    val audioUrl: String? = null,
    @ColumnInfo(name = "is_played", defaultValue = "0")
    val isPlayed: Boolean = false,
    @ColumnInfo(name = "is_new", defaultValue = "1")
    val isNew: Boolean = true,
    @ColumnInfo(name = "last_position_ms", defaultValue = "0")
    val lastPositionMs: Long = 0L
)

fun EpisodeEntity.toEpisode(): Episode = Episode().also {
    it.id = id
    it.feedId = feedId
    it.title = title
    it.description = description
    it.pubDate = pubDate
    it.duration = duration
    it.audioUrl = audioUrl
    it.isPlayed = isPlayed
    it.isNew = isNew
    it.lastPositionMs = lastPositionMs.coerceAtLeast(0L)
}

fun Episode.toEntity(feedIdOverride: Long? = null): EpisodeEntity = EpisodeEntity(
    id = id,
    feedId = feedIdOverride ?: feedId,
    title = title,
    description = description,
    pubDate = pubDate,
    duration = duration,
    audioUrl = audioUrl,
    isPlayed = isPlayed,
    isNew = isNew,
    lastPositionMs = lastPositionMs.coerceAtLeast(0L)
)
