package com.example.floodrisk.database

import androidx.room.*
import com.example.floodrisk.model.NotificationLog

@Dao
interface NotificationLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM notification_logs WHERE alertKey = :key)")
    fun hasNotificationBeenSent(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(log: NotificationLog): Long
}
