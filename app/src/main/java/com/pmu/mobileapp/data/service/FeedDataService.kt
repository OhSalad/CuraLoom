package com.pmu.mobileapp.data.service

import com.pmu.mobileapp.data.dao.FeedDao
import com.pmu.mobileapp.data.entity.toEntity
import com.pmu.mobileapp.data.entity.toFeed
import com.pmu.mobileapp.model.Feed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class FeedDataService(
    private val feedDao: FeedDao
) {
    fun insertFeed(feed: Feed): Long = blocking {
        feedDao.insert(feed.toEntity())
    }

    fun getAllFeeds(category: String?): List<Feed> = blocking {
        val feeds = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            feedDao.getAll()
        } else {
            feedDao.getByCategory(category)
        }
        feeds.map { it.toFeed() }
    }

    fun getFeedById(id: Long): Feed? = blocking {
        feedDao.getById(id)?.toFeed()
    }

    fun getFeedByUrl(url: String): Feed? = blocking {
        feedDao.getByUrl(url)?.toFeed()
    }

    fun getFeedCount(): Int = blocking {
        feedDao.count()
    }

    fun getAllCategories(): List<String> = blocking {
        feedDao.getAllCategories()
    }

    fun searchFeeds(query: String): List<Feed> = blocking {
        feedDao.search(query).map { it.toFeed() }
    }

    fun updateFeed(id: Long, newTitle: String?, newCategory: String?): Int = blocking {
        feedDao.updateFeed(id, newTitle, newCategory)
    }

    fun updateFeedFromImport(id: Long, title: String?, description: String?, author: String?): Int =
        blocking {
            feedDao.updateFeedFromImport(id, title, description, author)
        }

    fun markFeedNotNew(feedId: Long): Int = blocking {
        feedDao.markNotNew(feedId)
    }

    fun deleteFeed(id: Long): Int = blocking {
        feedDao.deleteById(id)
    }

    private fun <T> blocking(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
