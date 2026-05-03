package com.pmu.mobileapp.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pmu.mobileapp.BuildConfig
import com.pmu.mobileapp.data.ai.DefaultAiEpisodeCatalogProvider
import com.pmu.mobileapp.data.ai.GeminiPodcastAiService
import com.pmu.mobileapp.data.ai.GoogleGenAiJsonClient
import com.pmu.mobileapp.data.ai.PodcastAiConstants
import com.pmu.mobileapp.data.ai.PodcastAiService
import com.pmu.mobileapp.data.entity.FeedEntity
import com.pmu.mobileapp.data.repository.DefaultPodcastRepository
import com.pmu.mobileapp.data.repository.PodcastRepository
import com.pmu.mobileapp.data.service.EpisodeDataService
import com.pmu.mobileapp.data.service.FeedDataService
import com.pmu.mobileapp.data.service.LibraryDataService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    private val database: CuraLoomDatabase = Room.databaseBuilder(
        context.applicationContext,
        CuraLoomDatabase::class.java,
        DATABASE_NAME
    )
        .addMigrations(MIGRATION_7_8)
        .build()

    val feedService: FeedDataService = FeedDataService(database.feedDao())
    val episodeService: EpisodeDataService = EpisodeDataService(database.episodeDao())
    val libraryService: LibraryDataService = LibraryDataService(database.libraryDao())

    val podcastRepository: PodcastRepository = DefaultPodcastRepository(
        feedService = feedService,
        episodeService = episodeService,
        libraryService = libraryService
    )

    val podcastAiService: PodcastAiService = GeminiPodcastAiService(
        jsonClient = GoogleGenAiJsonClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            modelName = BuildConfig.GEMINI_MODEL.ifBlank { PodcastAiConstants.DEFAULT_MODEL }
        ),
        episodeCatalogProvider = DefaultAiEpisodeCatalogProvider(
            feedService = feedService,
            repository = podcastRepository
        )
    )

    init {
        seedDefaultFeedsIfNeeded()
    }

    private fun seedDefaultFeedsIfNeeded() {
        runBlocking(Dispatchers.IO) {
            if (database.feedDao().count() > 0) return@runBlocking
            database.feedDao().insertAll(
                DefaultPodcastFeeds.starterFeeds.map { feed ->
                    FeedEntity(
                        title = feed.title,
                        url = feed.url,
                        description = feed.description,
                        category = feed.category,
                        author = feed.author,
                        isNew = true
                    )
                }
            )
        }
    }

    private companion object {
        private const val DATABASE_NAME = "curaloom.db"

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE episodes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        feed_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        pub_date TEXT,
                        duration TEXT,
                        audio_url TEXT,
                        is_played INTEGER NOT NULL DEFAULT 0,
                        is_new INTEGER NOT NULL DEFAULT 1,
                        last_position_ms INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(feed_id) REFERENCES feeds(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO episodes_new (
                        id,
                        feed_id,
                        title,
                        description,
                        pub_date,
                        duration,
                        audio_url,
                        is_played,
                        is_new,
                        last_position_ms
                    )
                    SELECT
                        id,
                        feed_id,
                        title,
                        description,
                        pub_date,
                        duration,
                        audio_url,
                        is_played,
                        is_new,
                        last_position_ms
                    FROM episodes
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE episodes")
                db.execSQL("ALTER TABLE episodes_new RENAME TO episodes")
                db.execSQL("CREATE INDEX index_episodes_feed_id ON episodes(feed_id)")
            }
        }
    }
}
