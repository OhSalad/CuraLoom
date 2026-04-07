package com.pmu.mobileapp.data.service

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.Episode

class EpisodeDataService(
    private val dbHelper: DatabaseHelper
) {
    fun insertEpisode(episode: Episode): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.EPISODE_FEED_ID, episode.feedId)
            put(DatabaseHelper.EPISODE_TITLE, episode.title)
            put(DatabaseHelper.EPISODE_DESCRIPTION, episode.description)
            put(DatabaseHelper.EPISODE_PUB_DATE, episode.pubDate)
            put(DatabaseHelper.EPISODE_DURATION, episode.duration)
            put(DatabaseHelper.EPISODE_AUDIO_URL, episode.audioUrl)
            put(DatabaseHelper.EPISODE_IS_PLAYED, if (episode.isPlayed) 1 else 0)
            put(DatabaseHelper.EPISODE_IS_DOWNLOADED, if (episode.isDownloaded) 1 else 0)
            put(DatabaseHelper.EPISODE_IS_NEW, if (episode.isNew) 1 else 0)
            put(DatabaseHelper.EPISODE_LAST_POSITION_MS, episode.lastPositionMs.coerceAtLeast(0L))
        }
        return db.insert(DatabaseHelper.TABLE_EPISODES, null, values)
    }

    fun getEpisodeById(episodeId: Long): Episode? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_EPISODES} WHERE ${DatabaseHelper.EPISODE_ID} = ? LIMIT 1",
            arrayOf(episodeId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.toEpisode()
            }
        }
        return null
    }

    fun getEpisodesByFeedId(feedId: Long): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_EPISODES} WHERE ${DatabaseHelper.EPISODE_FEED_ID} = ? ORDER BY ${DatabaseHelper.EPISODE_PUB_DATE} DESC",
            arrayOf(feedId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                episodes.add(it.toEpisode())
            }
        }
        return episodes
    }

    fun replaceEpisodesForFeed(feedId: Long, episodes: List<Episode>) {
        val db = dbHelper.writableDatabase
        replaceEpisodesForFeed(db, feedId, episodes)
    }

    fun replaceEpisodesForFeed(db: SQLiteDatabase, feedId: Long, episodes: List<Episode>) {
        val ownsTransaction = !db.inTransaction()
        if (ownsTransaction) {
            db.beginTransaction()
        }
        try {
            data class EpisodeFlags(
                val isPlayed: Boolean,
                val isDownloaded: Boolean,
                val lastPositionMs: Long
            )

            val existingState = mutableMapOf<String, EpisodeFlags>()
            val existing = db.rawQuery(
                "SELECT ${DatabaseHelper.EPISODE_AUDIO_URL}, ${DatabaseHelper.EPISODE_IS_PLAYED}, ${DatabaseHelper.EPISODE_IS_DOWNLOADED}, ${DatabaseHelper.EPISODE_LAST_POSITION_MS} FROM ${DatabaseHelper.TABLE_EPISODES} WHERE ${DatabaseHelper.EPISODE_FEED_ID} = ?",
                arrayOf(feedId.toString())
            )
            existing.use {
                while (it.moveToNext()) {
                    val audioUrl = it.getString(0) ?: continue
                    val isPlayed = it.getInt(1) == 1
                    val isDownloaded = it.getInt(2) == 1
                    val lastPositionMs = it.getLong(3).coerceAtLeast(0L)
                    existingState[audioUrl] = EpisodeFlags(isPlayed, isDownloaded, lastPositionMs)
                }
            }

            db.delete(DatabaseHelper.TABLE_EPISODES, "${DatabaseHelper.EPISODE_FEED_ID} = ?", arrayOf(feedId.toString()))
            for (episode in episodes) {
                val state = episode.audioUrl?.let { existingState[it] }
                val values = ContentValues().apply {
                    put(DatabaseHelper.EPISODE_FEED_ID, feedId)
                    put(DatabaseHelper.EPISODE_TITLE, episode.title)
                    put(DatabaseHelper.EPISODE_DESCRIPTION, episode.description)
                    put(DatabaseHelper.EPISODE_PUB_DATE, episode.pubDate)
                    put(DatabaseHelper.EPISODE_DURATION, episode.duration)
                    put(DatabaseHelper.EPISODE_AUDIO_URL, episode.audioUrl)
                    put(DatabaseHelper.EPISODE_IS_PLAYED, if (state?.isPlayed == true) 1 else 0)
                    put(DatabaseHelper.EPISODE_IS_DOWNLOADED, if (state?.isDownloaded == true) 1 else 0)
                    put(DatabaseHelper.EPISODE_IS_NEW, if (episode.isNew) 1 else 0)
                    put(DatabaseHelper.EPISODE_LAST_POSITION_MS, state?.lastPositionMs ?: 0L)
                }
                db.insert(DatabaseHelper.TABLE_EPISODES, null, values)
            }
            if (ownsTransaction) {
                db.setTransactionSuccessful()
            }
        } finally {
            if (ownsTransaction) {
                db.endTransaction()
            }
        }
    }

    fun markEpisodePlayed(episodeId: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.EPISODE_IS_PLAYED, 1)
            put(DatabaseHelper.EPISODE_IS_NEW, 0)
        }
        return db.update(
            DatabaseHelper.TABLE_EPISODES,
            values,
            "${DatabaseHelper.EPISODE_ID} = ?",
            arrayOf(episodeId.toString())
        )
    }

    fun updateEpisodePlaybackPosition(episodeId: Long, positionMs: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.EPISODE_LAST_POSITION_MS, positionMs.coerceAtLeast(0L))
        }
        return db.update(
            DatabaseHelper.TABLE_EPISODES,
            values,
            "${DatabaseHelper.EPISODE_ID} = ?",
            arrayOf(episodeId.toString())
        )
    }

    fun deleteEpisode(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_EPISODES, "${DatabaseHelper.EPISODE_ID} = ?", arrayOf(id.toString()))
    }
}