package com.pmu.mobileapp.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.pmu.mobileapp.data.entity.LibraryEpisodeEntity

@Dao
interface LibraryDao {
    @Query(
        """
        SELECT
            e.id AS id,
            e.feed_id AS feed_id,
            f.title AS feed_title,
            e.title AS title,
            e.duration AS duration,
            e.pub_date AS pub_date,
            e.audio_url AS audio_url,
            e.last_position_ms AS last_position_ms,
            e.is_played AS is_played,
            e.is_new AS is_new
        FROM episodes e
        INNER JOIN feeds f ON f.id = e.feed_id
        ORDER BY e.pub_date DESC, e.id DESC
        """
    )
    fun getLibraryEpisodes(): List<LibraryEpisodeEntity>
}
