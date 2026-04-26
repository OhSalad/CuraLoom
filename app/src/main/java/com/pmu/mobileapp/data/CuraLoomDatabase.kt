package com.pmu.mobileapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pmu.mobileapp.data.dao.EpisodeDao
import com.pmu.mobileapp.data.dao.FeedDao
import com.pmu.mobileapp.data.dao.LibraryDao
import com.pmu.mobileapp.data.entity.EpisodeEntity
import com.pmu.mobileapp.data.entity.FeedEntity

@Database(
    entities = [
        FeedEntity::class,
        EpisodeEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class CuraLoomDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun libraryDao(): LibraryDao
}
