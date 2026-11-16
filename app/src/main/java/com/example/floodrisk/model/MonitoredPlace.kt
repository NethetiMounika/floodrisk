package com.example.floodrisk.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_places")
data class MonitoredPlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
