package com.pmu.mobileapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pmu.mobileapp.data.entity.EpisodeEntity

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(episode: EpisodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(episodes: List<EpisodeEntity>): List<Long>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    fun getById(episodeId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE feed_id = :feedId ORDER BY pub_date DESC")
    fun getByFeedId(feedId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE feed_id = :feedId AND audio_url IS NOT NULL")
    fun getStateByFeedId(feedId: Long): List<EpisodeEntity>

    @Query("DELETE FROM episodes WHERE feed_id = :feedId")
    fun deleteByFeedId(feedId: Long): Int

    @Transaction
    fun replaceForFeed(feedId: Long, episodes: List<EpisodeEntity>) {
        val existingState = getStateByFeedId(feedId).associateBy { it.audioUrl }
        deleteByFeedId(feedId)
        insertAll(
            episodes.map { episode ->
                val state = episode.audioUrl?.let { existingState[it] }
                episode.copy(
                    id = 0,
                    feedId = feedId,
                    isPlayed = state?.isPlayed ?: false,
                    lastPositionMs = state?.lastPositionMs?.coerceAtLeast(0L) ?: 0L
                )
            }
        )
    }

    @Query("UPDATE episodes SET is_played = 1, is_new = 0 WHERE id = :episodeId")
    fun markPlayed(episodeId: Long): Int

    @Query("UPDATE episodes SET last_position_ms = :positionMs WHERE id = :episodeId")
    fun updatePlaybackPosition(episodeId: Long, positionMs: Long): Int

    @Query("DELETE FROM episodes WHERE id = :id")
    fun deleteById(id: Long): Int
}
