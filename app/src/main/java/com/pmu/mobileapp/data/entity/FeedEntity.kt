package com.pmu.mobileapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pmu.mobileapp.model.Feed

@Entity(
    tableName = "feeds",
    indices = [Index(value = ["url"], unique = true)]
)
data class FeedEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "category", defaultValue = "'General'")
    val category: String? = "General",
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    @ColumnInfo(name = "author")
    val author: String? = null,
    @ColumnInfo(name = "is_new", defaultValue = "1")
    val isNew: Boolean = true,
    @ColumnInfo(name = "added_at", defaultValue = "(datetime('now'))")
    val addedAt: String? = null,
    @ColumnInfo(name = "last_refreshed", defaultValue = "(datetime('now'))")
    val lastRefreshed: String? = null
)

fun FeedEntity.toFeed(): Feed = Feed().also {
    it.id = id
    it.title = title
    it.url = url
    it.description = description
    it.category = category
    it.imageUrl = imageUrl
    it.author = author
    it.isNew = isNew
    it.addedAt = addedAt
    it.lastRefreshed = lastRefreshed
}

fun Feed.toEntity(): FeedEntity = FeedEntity(
    id = id,
    title = title,
    url = url,
    description = description,
    category = category ?: "General",
    imageUrl = imageUrl,
    author = author,
    isNew = isNew,
    addedAt = addedAt,
    lastRefreshed = lastRefreshed
)
