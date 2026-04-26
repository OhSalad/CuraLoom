package com.pmu.mobileapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pmu.mobileapp.data.entity.FeedEntity

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(feed: FeedEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(feeds: List<FeedEntity>): List<Long>

    @Query("SELECT * FROM feeds ORDER BY added_at DESC")
    fun getAll(): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE category = :category ORDER BY added_at DESC")
    fun getByCategory(category: String): List<FeedEntity>

    @Query("SELECT * FROM feeds WHERE id = :id LIMIT 1")
    fun getById(id: Long): FeedEntity?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): FeedEntity?

    @Query("SELECT COUNT(*) FROM feeds")
    fun count(): Int

    @Query("SELECT DISTINCT category FROM feeds WHERE category IS NOT NULL ORDER BY category")
    fun getAllCategories(): List<String>

    @Query("SELECT * FROM feeds WHERE title LIKE '%' || :query || '%' ORDER BY added_at DESC")
    fun search(query: String): List<FeedEntity>

    @Query(
        """
        UPDATE feeds
        SET title = :title,
            category = :category,
            last_refreshed = datetime('now')
        WHERE id = :id
        """
    )
    fun updateFeed(id: Long, title: String?, category: String?): Int

    @Query(
        """
        UPDATE feeds
        SET title = COALESCE(NULLIF(:title, ''), title),
            description = COALESCE(NULLIF(:description, ''), description),
            author = COALESCE(NULLIF(:author, ''), author),
            last_refreshed = datetime('now')
        WHERE id = :id
        """
    )
    fun updateFeedFromImport(
        id: Long,
        title: String?,
        description: String?,
        author: String?
    ): Int

    @Query("UPDATE feeds SET is_new = 0 WHERE id = :id")
    fun markNotNew(id: Long): Int

    @Query("DELETE FROM feeds WHERE id = :id")
    fun deleteById(id: Long): Int
}
