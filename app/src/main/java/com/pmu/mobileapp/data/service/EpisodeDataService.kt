package com.pmu.mobileapp.data.service

import com.pmu.mobileapp.data.dao.EpisodeDao
import com.pmu.mobileapp.data.entity.toEntity
import com.pmu.mobileapp.data.entity.toEpisode
import com.pmu.mobileapp.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class EpisodeDataService(
    private val episodeDao: EpisodeDao
) {
    fun insertEpisode(episode: Episode): Long = blocking {
        episodeDao.insert(episode.toEntity())
    }

    fun getEpisodeById(episodeId: Long): Episode? = blocking {
        episodeDao.getById(episodeId)?.toEpisode()
    }

    fun getEpisodesByFeedId(feedId: Long): List<Episode> = blocking {
        episodeDao.getByFeedId(feedId).map { it.toEpisode() }
    }

    fun replaceEpisodesForFeed(feedId: Long, episodes: List<Episode>) = blocking {
        episodeDao.replaceForFeed(
            feedId = feedId,
            episodes = episodes.map { it.toEntity(feedIdOverride = feedId) }
        )
    }

    fun markEpisodePlayed(episodeId: Long): Int = blocking {
        episodeDao.markPlayed(episodeId)
    }

    fun updateEpisodePlaybackPosition(episodeId: Long, positionMs: Long): Int = blocking {
        episodeDao.updatePlaybackPosition(episodeId, positionMs.coerceAtLeast(0L))
    }

    fun deleteEpisode(id: Long): Int = blocking {
        episodeDao.deleteById(id)
    }

    private fun <T> blocking(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
