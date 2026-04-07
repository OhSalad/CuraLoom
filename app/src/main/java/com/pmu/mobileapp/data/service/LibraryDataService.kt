package com.pmu.mobileapp.data.service

import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.LibraryEpisode

class LibraryDataService(
    private val dbHelper: DatabaseHelper
) {
    fun getLibraryEpisodes(): List<LibraryEpisode> {
        val episodes = mutableListOf<LibraryEpisode>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT
                e.${DatabaseHelper.EPISODE_ID},
                e.${DatabaseHelper.EPISODE_FEED_ID},
                f.${DatabaseHelper.FEED_TITLE},
                e.${DatabaseHelper.EPISODE_TITLE},
                e.${DatabaseHelper.EPISODE_DURATION},
                e.${DatabaseHelper.EPISODE_PUB_DATE},
                e.${DatabaseHelper.EPISODE_AUDIO_URL},
                e.${DatabaseHelper.EPISODE_LAST_POSITION_MS},
                e.${DatabaseHelper.EPISODE_IS_PLAYED},
                e.${DatabaseHelper.EPISODE_IS_DOWNLOADED},
                e.${DatabaseHelper.EPISODE_IS_NEW}
            FROM ${DatabaseHelper.TABLE_EPISODES} e
            INNER JOIN ${DatabaseHelper.TABLE_FEEDS} f ON f.${DatabaseHelper.FEED_ID} = e.${DatabaseHelper.EPISODE_FEED_ID}
            ORDER BY e.${DatabaseHelper.EPISODE_PUB_DATE} DESC, e.${DatabaseHelper.EPISODE_ID} DESC
            """.trimIndent(),
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                episodes.add(
                    LibraryEpisode(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_ID)),
                        feedId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_FEED_ID)),
                        feedTitle = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.FEED_TITLE)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_TITLE)),
                        duration = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_DURATION)),
                        pubDate = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_PUB_DATE)),
                        audioUrl = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_AUDIO_URL)),
                        lastPositionMs = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_LAST_POSITION_MS)).coerceAtLeast(0L),
                        isPlayed = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_PLAYED)) == 1,
                        isDownloaded = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_DOWNLOADED)) == 1,
                        isNew = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_NEW)) == 1
                    )
                )
            }
        }
        return episodes
    }
}