package com.example.floodrisk.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey
    val alertKey: String,
    val placeName: String,
    val alertDate: String,
    val timestamp: Long = System.currentTimeMillis()
)
