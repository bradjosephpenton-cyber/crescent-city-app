package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteTrack::class, SavedEstimate::class], version = 1, exportSchema = false)
abstract class BeatDatabase : RoomDatabase() {

    abstract fun beatDao(): BeatDao

    companion object {
        @Volatile
        private var INSTANCE: BeatDatabase? = null

        fun getDatabase(context: Context): BeatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BeatDatabase::class.java,
                    "crescent_beats_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
