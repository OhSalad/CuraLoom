package com.pmu.mobileapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pmu.mobileapp.data.service.EpisodeDataService
import com.pmu.mobileapp.data.service.FeedDataService
import com.pmu.mobileapp.data.service.LibraryDataService

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val feedService: FeedDataService by lazy { FeedDataService(this) }
    val episodeService: EpisodeDataService by lazy { EpisodeDataService(this) }
    val libraryService: LibraryDataService by lazy { LibraryDataService(this) }

    override fun onCreate(db: SQLiteDatabase) {
        val createFeedsTable = "CREATE TABLE $TABLE_FEEDS (" +
            "$FEED_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$FEED_TITLE TEXT NOT NULL, " +
            "$FEED_URL TEXT UNIQUE NOT NULL, " +
            "$FEED_DESCRIPTION TEXT, " +
            "$FEED_CATEGORY TEXT DEFAULT 'General', " +
            "$FEED_IMAGE_URL TEXT, " +
            "$FEED_AUTHOR TEXT, " +
            "$FEED_IS_NEW INTEGER DEFAULT 1, " +
            "$FEED_ADDED_AT TEXT DEFAULT (datetime('now')), " +
            "$FEED_LAST_REFRESHED TEXT DEFAULT (datetime('now'))" +
            ")"

        val createEpisodesTable = "CREATE TABLE $TABLE_EPISODES (" +
            "$EPISODE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$EPISODE_FEED_ID INTEGER NOT NULL, " +
            "$EPISODE_TITLE TEXT NOT NULL, " +
            "$EPISODE_DESCRIPTION TEXT, " +
            "$EPISODE_PUB_DATE TEXT, " +
            "$EPISODE_DURATION TEXT, " +
            "$EPISODE_AUDIO_URL TEXT, " +
            "$EPISODE_IS_PLAYED INTEGER DEFAULT 0, " +
            "$EPISODE_IS_DOWNLOADED INTEGER DEFAULT 0, " +
            "$EPISODE_IS_NEW INTEGER DEFAULT 1, " +
            "$EPISODE_LAST_POSITION_MS INTEGER DEFAULT 0, " +
            "FOREIGN KEY ($EPISODE_FEED_ID) REFERENCES $TABLE_FEEDS($FEED_ID) ON DELETE CASCADE" +
            ")"

        db.execSQL(createFeedsTable)
        db.execSQL(createEpisodesTable)
        seedDefaultFeeds(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_EPISODES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FEEDS")
            onCreate(db)
            return
        }

        if (oldVersion < 2) {
            removePlaceholderFeeds(db)
        }

        if (oldVersion < 3) {
            db.execSQL(
                "UPDATE $TABLE_EPISODES SET $EPISODE_AUDIO_URL = ? WHERE $EPISODE_AUDIO_URL IS NULL OR TRIM($EPISODE_AUDIO_URL) = ''",
                arrayOf("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            )
        }

        if (oldVersion < 4) {
            db.execSQL(
                "ALTER TABLE $TABLE_EPISODES ADD COLUMN $EPISODE_LAST_POSITION_MS INTEGER DEFAULT 0"
            )
            db.execSQL(
                "UPDATE $TABLE_EPISODES SET $EPISODE_LAST_POSITION_MS = 0 WHERE $EPISODE_LAST_POSITION_MS IS NULL"
            )
        }

        if (oldVersion < 5) {
            removePlaceholderFeeds(db)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun removePlaceholderFeeds(db: SQLiteDatabase) {
        val cursor = db.rawQuery(
            "SELECT $FEED_ID FROM $TABLE_FEEDS WHERE $FEED_URL LIKE ? OR $FEED_URL IN (?, ?, ?)",
            arrayOf(
                "%example.com%",
                "https://feeds.npr.org/510289/podcast.xml",
                "https://feeds.megaphone.fm/vergecast",
                "https://feeds.feedburner.com/TEDTalks_audio"
            )
        )
        val ids = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                ids.add(it.getLong(0))
            }
        }

        for (feedId in ids) {
            db.delete(TABLE_EPISODES, "$EPISODE_FEED_ID = ?", arrayOf(feedId.toString()))
            db.delete(TABLE_FEEDS, "$FEED_ID = ?", arrayOf(feedId.toString()))
        }
    }

    private fun seedDefaultFeeds(db: SQLiteDatabase) {
        DefaultPodcastFeeds.starterFeeds.forEach { feed ->
            db.execSQL(
                "INSERT OR IGNORE INTO $TABLE_FEEDS (" +
                    "$FEED_TITLE, $FEED_URL, $FEED_DESCRIPTION, $FEED_CATEGORY, $FEED_AUTHOR, $FEED_IS_NEW" +
                    ") VALUES (?, ?, ?, ?, ?, 1)",
                arrayOf(feed.title, feed.url, feed.description, feed.category, feed.author)
            )
        }
    }

    fun <T> withTransaction(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val result = block(db)
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "curaloom.db"
        private const val DATABASE_VERSION = 6

        const val TABLE_FEEDS = "feeds"
        const val FEED_ID = "id"
        const val FEED_TITLE = "title"
        const val FEED_URL = "url"
        const val FEED_DESCRIPTION = "description"
        const val FEED_CATEGORY = "category"
        const val FEED_IMAGE_URL = "image_url"
        const val FEED_AUTHOR = "author"
        const val FEED_IS_NEW = "is_new"
        const val FEED_ADDED_AT = "added_at"
        const val FEED_LAST_REFRESHED = "last_refreshed"

        const val TABLE_EPISODES = "episodes"
        const val EPISODE_ID = "id"
        const val EPISODE_FEED_ID = "feed_id"
        const val EPISODE_TITLE = "title"
        const val EPISODE_DESCRIPTION = "description"
        const val EPISODE_PUB_DATE = "pub_date"
        const val EPISODE_DURATION = "duration"
        const val EPISODE_AUDIO_URL = "audio_url"
        const val EPISODE_IS_PLAYED = "is_played"
        const val EPISODE_IS_DOWNLOADED = "is_downloaded"
        const val EPISODE_IS_NEW = "is_new"
        const val EPISODE_LAST_POSITION_MS = "last_position_ms"

        @Volatile
        private var instance: DatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
