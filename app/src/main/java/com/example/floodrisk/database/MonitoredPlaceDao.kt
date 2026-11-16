package com.example.floodrisk.database

import androidx.room.*
import com.example.floodrisk.model.MonitoredPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredPlaceDao {
    @Query("SELECT * FROM monitored_places ORDER BY isDefault DESC, name ASC")
    fun getAllPlacesFlow(): Flow<List<MonitoredPlace>>

    @Query("SELECT * FROM monitored_places ORDER BY isDefault DESC, name ASC")
    fun getAllPlaces(): List<MonitoredPlace>

    @Query("SELECT * FROM monitored_places WHERE id = :id LIMIT 1")
    fun getPlaceById(id: Long): MonitoredPlace?

    @Query("SELECT * FROM monitored_places WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPlace(): MonitoredPlace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlace(place: MonitoredPlace): Long

    @Update
    fun updatePlace(place: MonitoredPlace): Int

    @Delete
    fun deletePlace(place: MonitoredPlace): Int

    @Query("UPDATE monitored_places SET isDefault = 0")
    fun clearDefaultFlags(): Int
}
