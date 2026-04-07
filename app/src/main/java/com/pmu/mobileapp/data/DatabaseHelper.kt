package com.pmu.mobileapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed
import com.pmu.mobileapp.model.LibraryEpisode

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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
            "FOREIGN KEY ($EPISODE_FEED_ID) REFERENCES $TABLE_FEEDS($FEED_ID) ON DELETE CASCADE" +
            ")"

        db.execSQL(createFeedsTable)
        db.execSQL(createEpisodesTable)
        seedInitialLibraryData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            removeExampleFeeds(db)
            seedInitialLibraryData(db)
            return
        }

        if (oldVersion < 3) {
            db.execSQL(
                "UPDATE $TABLE_EPISODES SET $EPISODE_AUDIO_URL = ? WHERE $EPISODE_AUDIO_URL IS NULL OR TRIM($EPISODE_AUDIO_URL) = ''",
                arrayOf("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            )
            return
        }

        db.execSQL("DROP TABLE IF EXISTS $TABLE_EPISODES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEEDS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun insertFeed(feed: Feed): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(FEED_TITLE, feed.title)
            put(FEED_URL, feed.url)
            put(FEED_DESCRIPTION, feed.description)
            put(FEED_CATEGORY, feed.category)
            put(FEED_IMAGE_URL, feed.imageUrl)
            put(FEED_AUTHOR, feed.author)
            put(FEED_IS_NEW, if (feed.isNew) 1 else 0)
        }
        return db.insert(TABLE_FEEDS, null, values)
    }

    fun insertEpisode(episode: Episode): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(EPISODE_FEED_ID, episode.feedId)
            put(EPISODE_TITLE, episode.title)
            put(EPISODE_DESCRIPTION, episode.description)
            put(EPISODE_PUB_DATE, episode.pubDate)
            put(EPISODE_DURATION, episode.duration)
            put(EPISODE_AUDIO_URL, episode.audioUrl)
            put(EPISODE_IS_PLAYED, if (episode.isPlayed) 1 else 0)
            put(EPISODE_IS_DOWNLOADED, if (episode.isDownloaded) 1 else 0)
            put(EPISODE_IS_NEW, if (episode.isNew) 1 else 0)
        }
        return db.insert(TABLE_EPISODES, null, values)
    }

    fun getAllFeeds(category: String?): List<Feed> {
        val feeds = mutableListOf<Feed>()
        val db = readableDatabase

        val (query, args) = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            "SELECT * FROM $TABLE_FEEDS ORDER BY $FEED_ADDED_AT DESC" to null
        } else {
            "SELECT * FROM $TABLE_FEEDS WHERE $FEED_CATEGORY = ? ORDER BY $FEED_ADDED_AT DESC" to arrayOf(category)
        }

        val cursor = db.rawQuery(query, args)
        cursor.use {
            while (it.moveToNext()) {
                feeds.add(cursorToFeed(it))
            }
        }
        return feeds
    }

    fun getFeedById(id: Long): Feed? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_FEEDS WHERE $FEED_ID = ?",
            arrayOf(id.toString())
        )
        cursor.use {
            return if (it.moveToFirst()) cursorToFeed(it) else null
        }
    }

    fun getEpisodesByFeedId(feedId: Long): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_EPISODES WHERE $EPISODE_FEED_ID = ? ORDER BY $EPISODE_PUB_DATE DESC",
            arrayOf(feedId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                episodes.add(cursorToEpisode(it))
            }
        }
        return episodes
    }

    fun getLibraryEpisodes(): List<LibraryEpisode> {
        val episodes = mutableListOf<LibraryEpisode>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT
                e.$EPISODE_ID,
                e.$EPISODE_FEED_ID,
                f.$FEED_TITLE,
                e.$EPISODE_TITLE,
                e.$EPISODE_DURATION,
                e.$EPISODE_PUB_DATE,
                e.$EPISODE_AUDIO_URL,
                e.$EPISODE_IS_PLAYED,
                e.$EPISODE_IS_DOWNLOADED,
                e.$EPISODE_IS_NEW
            FROM $TABLE_EPISODES e
            INNER JOIN $TABLE_FEEDS f ON f.$FEED_ID = e.$EPISODE_FEED_ID
            ORDER BY e.$EPISODE_PUB_DATE DESC, e.$EPISODE_ID DESC
            """.trimIndent(),
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                episodes.add(
                    LibraryEpisode(
                        id = it.getLong(it.getColumnIndexOrThrow(EPISODE_ID)),
                        feedId = it.getLong(it.getColumnIndexOrThrow(EPISODE_FEED_ID)),
                        feedTitle = it.getString(it.getColumnIndexOrThrow(FEED_TITLE)),
                        title = it.getString(it.getColumnIndexOrThrow(EPISODE_TITLE)),
                        duration = it.getString(it.getColumnIndexOrThrow(EPISODE_DURATION)),
                        pubDate = it.getString(it.getColumnIndexOrThrow(EPISODE_PUB_DATE)),
                        audioUrl = it.getString(it.getColumnIndexOrThrow(EPISODE_AUDIO_URL)),
                        isPlayed = it.getInt(it.getColumnIndexOrThrow(EPISODE_IS_PLAYED)) == 1,
                        isDownloaded = it.getInt(it.getColumnIndexOrThrow(EPISODE_IS_DOWNLOADED)) == 1,
                        isNew = it.getInt(it.getColumnIndexOrThrow(EPISODE_IS_NEW)) == 1
                    )
                )
            }
        }
        return episodes
    }

    fun getFeedCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FEEDS", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getAllCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $FEED_CATEGORY FROM $TABLE_FEEDS ORDER BY $FEED_CATEGORY",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                categories.add(it.getString(0))
            }
        }
        return categories
    }

    fun searchFeeds(query: String): List<Feed> {
        val feeds = mutableListOf<Feed>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_FEEDS WHERE $FEED_TITLE LIKE ? ORDER BY $FEED_ADDED_AT DESC",
            arrayOf("%$query%")
        )
        cursor.use {
            while (it.moveToNext()) {
                feeds.add(cursorToFeed(it))
            }
        }
        return feeds
    }

    fun updateFeed(id: Long, newTitle: String?, newCategory: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(FEED_TITLE, newTitle)
            put(FEED_CATEGORY, newCategory)
        }
        val updated = db.update(TABLE_FEEDS, values, "$FEED_ID = ?", arrayOf(id.toString()))
        db.execSQL(
            "UPDATE $TABLE_FEEDS SET $FEED_LAST_REFRESHED = datetime('now') WHERE $FEED_ID = ?",
            arrayOf(id.toString())
        )
        return updated
    }

    fun updateFeedFromImport(id: Long, title: String?, description: String?, author: String?, resolvedUrl: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (!title.isNullOrBlank()) put(FEED_TITLE, title)
            if (!description.isNullOrBlank()) put(FEED_DESCRIPTION, description)
            if (!author.isNullOrBlank()) put(FEED_AUTHOR, author)
            if (!resolvedUrl.isNullOrBlank()) put(FEED_URL, resolvedUrl)
        }
        if (values.size() > 0) {
            db.update(TABLE_FEEDS, values, "$FEED_ID = ?", arrayOf(id.toString()))
        }
        db.execSQL(
            "UPDATE $TABLE_FEEDS SET $FEED_LAST_REFRESHED = datetime('now') WHERE $FEED_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun replaceEpisodesForFeed(feedId: Long, episodes: List<Episode>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existingState = mutableMapOf<String, Pair<Boolean, Boolean>>()
            val existing = db.rawQuery(
                "SELECT $EPISODE_AUDIO_URL, $EPISODE_IS_PLAYED, $EPISODE_IS_DOWNLOADED FROM $TABLE_EPISODES WHERE $EPISODE_FEED_ID = ?",
                arrayOf(feedId.toString())
            )
            existing.use {
                while (it.moveToNext()) {
                    val audioUrl = it.getString(0) ?: continue
                    val isPlayed = it.getInt(1) == 1
                    val isDownloaded = it.getInt(2) == 1
                    existingState[audioUrl] = isPlayed to isDownloaded
                }
            }

            db.delete(TABLE_EPISODES, "$EPISODE_FEED_ID = ?", arrayOf(feedId.toString()))
            for (episode in episodes) {
                val state = episode.audioUrl?.let { existingState[it] }
                val values = ContentValues().apply {
                    put(EPISODE_FEED_ID, feedId)
                    put(EPISODE_TITLE, episode.title)
                    put(EPISODE_DESCRIPTION, episode.description)
                    put(EPISODE_PUB_DATE, episode.pubDate)
                    put(EPISODE_DURATION, episode.duration)
                    put(EPISODE_AUDIO_URL, episode.audioUrl)
                    put(EPISODE_IS_PLAYED, if (state?.first == true) 1 else 0)
                    put(EPISODE_IS_DOWNLOADED, if (state?.second == true) 1 else 0)
                    put(EPISODE_IS_NEW, if (episode.isNew) 1 else 0)
                }
                db.insert(TABLE_EPISODES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun markEpisodePlayed(episodeId: Long): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(EPISODE_IS_PLAYED, 1)
            put(EPISODE_IS_NEW, 0)
        }
        return db.update(TABLE_EPISODES, values, "$EPISODE_ID = ?", arrayOf(episodeId.toString()))
    }

    fun markFeedNotNew(feedId: Long): Int {
        val db = writableDatabase
        val values = ContentValues().apply { put(FEED_IS_NEW, 0) }
        return db.update(TABLE_FEEDS, values, "$FEED_ID = ?", arrayOf(feedId.toString()))
    }

    fun deleteFeed(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_FEEDS, "$FEED_ID = ?", arrayOf(id.toString()))
    }

    fun deleteEpisode(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_EPISODES, "$EPISODE_ID = ?", arrayOf(id.toString()))
    }

    private fun cursorToFeed(cursor: Cursor): Feed {
        return Feed().apply {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(FEED_ID))
            title = cursor.getString(cursor.getColumnIndexOrThrow(FEED_TITLE))
            url = cursor.getString(cursor.getColumnIndexOrThrow(FEED_URL))
            description = cursor.getString(cursor.getColumnIndexOrThrow(FEED_DESCRIPTION))
            category = cursor.getString(cursor.getColumnIndexOrThrow(FEED_CATEGORY))
            imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(FEED_IMAGE_URL))
            author = cursor.getString(cursor.getColumnIndexOrThrow(FEED_AUTHOR))
            isNew = cursor.getInt(cursor.getColumnIndexOrThrow(FEED_IS_NEW)) == 1
            addedAt = cursor.getString(cursor.getColumnIndexOrThrow(FEED_ADDED_AT))
            lastRefreshed = cursor.getString(cursor.getColumnIndexOrThrow(FEED_LAST_REFRESHED))
        }
    }

    private fun cursorToEpisode(cursor: Cursor): Episode {
        return Episode().apply {
            id = cursor.getLong(cursor.getColumnIndexOrThrow(EPISODE_ID))
            feedId = cursor.getLong(cursor.getColumnIndexOrThrow(EPISODE_FEED_ID))
            title = cursor.getString(cursor.getColumnIndexOrThrow(EPISODE_TITLE))
            description = cursor.getString(cursor.getColumnIndexOrThrow(EPISODE_DESCRIPTION))
            pubDate = cursor.getString(cursor.getColumnIndexOrThrow(EPISODE_PUB_DATE))
            duration = cursor.getString(cursor.getColumnIndexOrThrow(EPISODE_DURATION))
            audioUrl = cursor.getString(cursor.getColumnIndexOrThrow(EPISODE_AUDIO_URL))
            isPlayed = cursor.getInt(cursor.getColumnIndexOrThrow(EPISODE_IS_PLAYED)) == 1
            isDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(EPISODE_IS_DOWNLOADED)) == 1
            isNew = cursor.getInt(cursor.getColumnIndexOrThrow(EPISODE_IS_NEW)) == 1
        }
    }

    private fun seedInitialLibraryData(db: SQLiteDatabase) {
        val planetMoneyId = insertFeedIfMissing(
            db,
            "NPR Planet Money",
            "https://feeds.npr.org/510289/podcast.xml",
            "The economy, explained through stories you can use.",
            "Science",
            "NPR",
            false
        )
        seedEpisodesIfEmpty(
            db,
            planetMoneyId,
            arrayOf(
                arrayOf("The Indicator Mix Tape", "A roundup of recent economic stories and market signals.", "Jan 10, 2026", "28 min", "1", "0", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
                arrayOf("Why Prices Move", "A practical breakdown of inflation, supply, and demand.", "Jan 03, 2026", "31 min", "0", "0", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
                arrayOf("Work, Wages, and AI", "How automation trends are reshaping jobs around the world.", "Dec 27, 2025", "34 min", "0", "1", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
            )
        )

        val vergecastId = insertFeedIfMissing(
            db,
            "The Vergecast",
            "https://feeds.megaphone.fm/vergecast",
            "Tech news, reviews, and interviews from The Verge.",
            "Technology",
            "The Verge",
            false
        )
        seedEpisodesIfEmpty(
            db,
            vergecastId,
            arrayOf(
                arrayOf("Big Week in Devices", "A walkthrough of this week's major hardware launches.", "Jan 09, 2026", "52 min", "1", "0", "1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
                arrayOf("The Future of Mobile", "What the next cycle of smartphones and AI assistants looks like.", "Jan 02, 2026", "47 min", "0", "0", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
                arrayOf("Platform Wars in 2026", "Who is winning in streaming, app stores, and ecosystems.", "Dec 26, 2025", "49 min", "0", "1", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3")
            )
        )

        val tedId = insertFeedIfMissing(
            db,
            "TED Talks Daily",
            "https://feeds.feedburner.com/TEDTalks_audio",
            "Daily talks from TED on technology, design, and ideas.",
            "Design",
            "TED",
            false
        )
        seedEpisodesIfEmpty(
            db,
            tedId,
            arrayOf(
                arrayOf("Designing for Human Time", "How small design choices can improve focus and wellbeing.", "Jan 08, 2026", "19 min", "1", "0", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
                arrayOf("Building Useful AI", "A practical conversation about responsible AI products.", "Jan 01, 2026", "22 min", "0", "0", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
                arrayOf("Cities for Everyone", "How urban design changes accessibility and belonging.", "Dec 25, 2025", "18 min", "0", "1", "0", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3")
            )
        )
    }

    private fun removeExampleFeeds(db: SQLiteDatabase) {
        val cursor = db.rawQuery(
            "SELECT $FEED_ID FROM $TABLE_FEEDS WHERE $FEED_URL LIKE ?",
            arrayOf("%example.com%")
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

    private fun insertFeedIfMissing(
        db: SQLiteDatabase,
        title: String,
        url: String,
        description: String,
        category: String,
        author: String,
        isNew: Boolean
    ): Long {
        val existing = db.rawQuery(
            "SELECT $FEED_ID FROM $TABLE_FEEDS WHERE $FEED_URL = ?",
            arrayOf(url)
        )
        existing.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }

        val values = ContentValues().apply {
            put(FEED_TITLE, title)
            put(FEED_URL, url)
            put(FEED_DESCRIPTION, description)
            put(FEED_CATEGORY, category)
            put(FEED_AUTHOR, author)
            put(FEED_IS_NEW, if (isNew) 1 else 0)
        }
        return db.insert(TABLE_FEEDS, null, values)
    }

    private fun seedEpisodesIfEmpty(db: SQLiteDatabase, feedId: Long, episodes: Array<Array<String>>) {
        if (feedId == -1L) return

        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EPISODES WHERE $EPISODE_FEED_ID = ?",
            arrayOf(feedId.toString())
        )
        val hasEpisodes = cursor.use { it.moveToFirst() && it.getInt(0) > 0 }
        if (hasEpisodes) return

        for (ep in episodes) {
            val values = ContentValues().apply {
                put(EPISODE_FEED_ID, feedId)
                put(EPISODE_TITLE, ep[0])
                put(EPISODE_DESCRIPTION, ep[1])
                put(EPISODE_PUB_DATE, ep[2])
                put(EPISODE_DURATION, ep[3])
                put(EPISODE_IS_NEW, ep[4].toInt())
                put(EPISODE_IS_PLAYED, ep[5].toInt())
                put(EPISODE_IS_DOWNLOADED, ep[6].toInt())
                put(EPISODE_AUDIO_URL, ep[7])
            }
            db.insert(TABLE_EPISODES, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "curaloom.db"
        private const val DATABASE_VERSION = 3

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
