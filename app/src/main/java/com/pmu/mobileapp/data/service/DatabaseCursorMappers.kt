package com.pmu.mobileapp.data.service

import android.database.Cursor
import com.pmu.mobileapp.data.DatabaseHelper
import com.pmu.mobileapp.model.Episode
import com.pmu.mobileapp.model.Feed

internal fun Cursor.toFeed(): Feed {
    return Feed().apply {
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.FEED_ID))
        title = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_TITLE))
        url = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_URL))
        description = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_DESCRIPTION))
        category = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_CATEGORY))
        imageUrl = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_IMAGE_URL))
        author = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_AUTHOR))
        isNew = getInt(getColumnIndexOrThrow(DatabaseHelper.FEED_IS_NEW)) == 1
        addedAt = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_ADDED_AT))
        lastRefreshed = getString(getColumnIndexOrThrow(DatabaseHelper.FEED_LAST_REFRESHED))
    }
}

internal fun Cursor.toEpisode(): Episode {
    return Episode().apply {
        id = getLong(getColumnIndexOrThrow(DatabaseHelper.EPISODE_ID))
        feedId = getLong(getColumnIndexOrThrow(DatabaseHelper.EPISODE_FEED_ID))
        title = getString(getColumnIndexOrThrow(DatabaseHelper.EPISODE_TITLE))
        description = getString(getColumnIndexOrThrow(DatabaseHelper.EPISODE_DESCRIPTION))
        pubDate = getString(getColumnIndexOrThrow(DatabaseHelper.EPISODE_PUB_DATE))
        duration = getString(getColumnIndexOrThrow(DatabaseHelper.EPISODE_DURATION))
        audioUrl = getString(getColumnIndexOrThrow(DatabaseHelper.EPISODE_AUDIO_URL))
        isPlayed = getInt(getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_PLAYED)) == 1
        isDownloaded = getInt(getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_DOWNLOADED)) == 1
        isNew = getInt(getColumnIndexOrThrow(DatabaseHelper.EPISODE_IS_NEW)) == 1
        lastPositionMs = getLong(getColumnIndexOrThrow(DatabaseHelper.EPISODE_LAST_POSITION_MS)).coerceAtLeast(0L)
    }
}