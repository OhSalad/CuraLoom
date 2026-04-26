package com.pmu.mobileapp.data.service

import com.pmu.mobileapp.data.dao.LibraryDao
import com.pmu.mobileapp.data.entity.toLibraryEpisode
import com.pmu.mobileapp.model.LibraryEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LibraryDataService(
    private val libraryDao: LibraryDao
) {
    fun getLibraryEpisodes(): List<LibraryEpisode> = runBlocking(Dispatchers.IO) {
        libraryDao.getLibraryEpisodes().map { it.toLibraryEpisode() }
    }
}
