package com.example.floodrisk.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.floodrisk.R
import com.example.floodrisk.database.AppDatabase
import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.NotificationLog
import com.example.floodrisk.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "river_flood_alerts_channel"
        const val CHANNEL_NAME = "River Flood Alerts"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for severe river flood alerts within 7 days."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun notifyFloodAlert(
        placeName: String,
        alertDay: FloodForecastDay
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasNotificationPermission()) return@withContext false

        val alertKey = "${placeName}_${alertDay.date}"
        val db = AppDatabase.getDatabase(context)
        val alreadySent = db.notificationLogDao().hasNotificationBeenSent(alertKey)

        if (alreadySent) {
            return@withContext false
        }

        val dischargeStr = alertDay.riverDischarge?.let { String.format(java.util.Locale.US, "%.2f m³/s", it) }
            ?: alertDay.riverDischargeMax?.let { String.format(java.util.Locale.US, "%.2f m³/s", it) }
            ?: "High"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alertKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationContent = "Alert-level flood risk expected at $placeName on ${alertDay.date}. " +
                "Forecast discharge: $dischargeStr. Please monitor local conditions closely."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flood_alert)
            .setContentTitle("River Flood Alert")
            .setContentText("Flood Alert for $placeName on ${alertDay.date}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alertKey.hashCode(), notification)

        db.notificationLogDao().insertLog(
            NotificationLog(
                alertKey = alertKey,
                placeName = placeName,
                alertDate = alertDay.date
            )
        )

        return@withContext true
    }
}
