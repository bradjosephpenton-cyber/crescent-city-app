package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BeatDao {

    // Favorites Management
    @Query("SELECT * FROM favorite_tracks ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteTrack)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteTrack)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteFavoriteById(trackId: String)


    // Estimates Management
    @Query("SELECT * FROM saved_estimates ORDER BY timestamp DESC")
    fun getAllEstimates(): Flow<List<SavedEstimate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: SavedEstimate)

    @Delete
    suspend fun deleteEstimate(estimate: SavedEstimate)

    @Query("DELETE FROM saved_estimates WHERE id = :estimateId")
    suspend fun deleteEstimateById(estimateId: Int)
}
