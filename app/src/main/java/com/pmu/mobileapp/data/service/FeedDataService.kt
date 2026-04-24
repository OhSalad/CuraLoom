package com.pmu.mobileapp.data.service

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.Feed

class FeedDataService(
    private val dbHelper: DatabaseHelper
) {
    fun insertFeed(feed: Feed): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.FEED_TITLE, feed.title)
            put(DatabaseHelper.FEED_URL, feed.url)
            put(DatabaseHelper.FEED_DESCRIPTION, feed.description)
            put(DatabaseHelper.FEED_CATEGORY, feed.category)
            put(DatabaseHelper.FEED_IMAGE_URL, feed.imageUrl)
            put(DatabaseHelper.FEED_AUTHOR, feed.author)
            put(DatabaseHelper.FEED_IS_NEW, if (feed.isNew) 1 else 0)
        }
        return db.insert(DatabaseHelper.TABLE_FEEDS, null, values)
    }

    fun getAllFeeds(category: String?): List<Feed> {
        val feeds = mutableListOf<Feed>()
        val db = dbHelper.readableDatabase

        val (query, args) = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            "SELECT * FROM ${DatabaseHelper.TABLE_FEEDS} ORDER BY ${DatabaseHelper.FEED_ADDED_AT} DESC" to null
        } else {
            "SELECT * FROM ${DatabaseHelper.TABLE_FEEDS} WHERE ${DatabaseHelper.FEED_CATEGORY} = ? ORDER BY ${DatabaseHelper.FEED_ADDED_AT} DESC" to arrayOf(category)
        }

        val cursor = db.rawQuery(query, args)
        cursor.use {
            while (it.moveToNext()) {
                feeds.add(it.toFeed())
            }
        }
        return feeds
    }

    fun getFeedById(id: Long): Feed? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_FEEDS} WHERE ${DatabaseHelper.FEED_ID} = ?",
            arrayOf(id.toString())
        )
        cursor.use {
            return if (it.moveToFirst()) it.toFeed() else null
        }
    }

    fun getFeedByUrl(url: String): Feed? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_FEEDS} WHERE ${DatabaseHelper.FEED_URL} = ?",
            arrayOf(url)
        )
        cursor.use {
            return if (it.moveToFirst()) it.toFeed() else null
        }
    }

    fun getFeedCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_FEEDS}", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun getAllCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT ${DatabaseHelper.FEED_CATEGORY} FROM ${DatabaseHelper.TABLE_FEEDS} ORDER BY ${DatabaseHelper.FEED_CATEGORY}",
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
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_FEEDS} WHERE ${DatabaseHelper.FEED_TITLE} LIKE ? ORDER BY ${DatabaseHelper.FEED_ADDED_AT} DESC",
            arrayOf("%$query%")
        )
        cursor.use {
            while (it.moveToNext()) {
                feeds.add(it.toFeed())
            }
        }
        return feeds
    }

    fun updateFeed(id: Long, newTitle: String?, newCategory: String?): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.FEED_TITLE, newTitle)
            put(DatabaseHelper.FEED_CATEGORY, newCategory)
        }
        val updated = db.update(
            DatabaseHelper.TABLE_FEEDS,
            values,
            "${DatabaseHelper.FEED_ID} = ?",
            arrayOf(id.toString())
        )
        updateLastRefreshed(db, id)
        return updated
    }

    fun updateFeedFromImport(id: Long, title: String?, description: String?, author: String?, resolvedUrl: String?) {
        val db = dbHelper.writableDatabase
        updateFeedFromImport(db, id, title, description, author, resolvedUrl)
    }

    fun updateFeedFromImport(
        db: SQLiteDatabase,
        id: Long,
        title: String?,
        description: String?,
        author: String?,
        resolvedUrl: String?
    ) {
        val values = ContentValues().apply {
            if (!title.isNullOrBlank()) put(DatabaseHelper.FEED_TITLE, title)
            if (!description.isNullOrBlank()) put(DatabaseHelper.FEED_DESCRIPTION, description)
            if (!author.isNullOrBlank()) put(DatabaseHelper.FEED_AUTHOR, author)
            if (!resolvedUrl.isNullOrBlank()) put(DatabaseHelper.FEED_URL, resolvedUrl)
        }
        if (values.size() > 0) {
            db.update(
                DatabaseHelper.TABLE_FEEDS,
                values,
                "${DatabaseHelper.FEED_ID} = ?",
                arrayOf(id.toString())
            )
        }
        updateLastRefreshed(db, id)
    }

    fun markFeedNotNew(feedId: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put(DatabaseHelper.FEED_IS_NEW, 0) }
        return db.update(
            DatabaseHelper.TABLE_FEEDS,
            values,
            "${DatabaseHelper.FEED_ID} = ?",
            arrayOf(feedId.toString())
        )
    }

    fun deleteFeed(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_FEEDS, "${DatabaseHelper.FEED_ID} = ?", arrayOf(id.toString()))
    }

    private fun updateLastRefreshed(db: SQLiteDatabase, id: Long) {
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_FEEDS} SET ${DatabaseHelper.FEED_LAST_REFRESHED} = datetime('now') WHERE ${DatabaseHelper.FEED_ID} = ?",
            arrayOf(id.toString())
        )
    }
}
