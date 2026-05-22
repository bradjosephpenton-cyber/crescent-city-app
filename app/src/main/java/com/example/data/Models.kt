package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room entities to keep track of user selections and dynamic licensing estimates
@Entity(tableName = "favorite_tracks")
data class FavoriteTrack(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_estimates")
data class SavedEstimate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: String,
    val trackName: String,
    val licenseType: String,
    val basePrice: Double,
    val vocalRecordingAddon: Boolean,
    val radioPlacementsAddon: Boolean,
    val musicVideoAddon: Boolean,
    val totalEstimate: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// In-Memory Representation of static track configurations fetched from the artist page
data class Track(
    val id: String,
    val title: String,
    val durationText: String, // e.g. "3:04"
    val durationMs: Long,
    val description: String,
    val streamUrl: String,
    val imageUrl: String,
    val datePublished: String,
    val price: Double = 25.0,
    val tags: List<String> = emptyList()
)
