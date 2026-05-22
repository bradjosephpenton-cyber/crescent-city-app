package com.example.data

import kotlinx.coroutines.flow.Flow

class BeatRepository(private val beatDao: BeatDao) {

    val allFavorites: Flow<List<FavoriteTrack>> = beatDao.getAllFavorites()
    val allEstimates: Flow<List<SavedEstimate>> = beatDao.getAllEstimates()

    suspend fun addFavorite(trackId: String) {
        beatDao.insertFavorite(FavoriteTrack(id = trackId))
    }

    suspend fun removeFavorite(trackId: String) {
        beatDao.deleteFavoriteById(trackId)
    }

    suspend fun saveEstimate(estimate: SavedEstimate) {
        beatDao.insertEstimate(estimate)
    }

    suspend fun deleteEstimate(estimate: SavedEstimate) {
        beatDao.deleteEstimate(estimate)
    }

    suspend fun deleteEstimateById(id: Int) {
        beatDao.deleteEstimateById(id)
    }
}
