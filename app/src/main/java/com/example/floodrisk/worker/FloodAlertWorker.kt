package com.example.floodrisk.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.floodrisk.database.AppDatabase
import com.example.floodrisk.notification.NotificationHelper
import com.example.floodrisk.repository.FloodRepository
import com.example.floodrisk.utils.RiskCalculator

class FloodAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(context)
            val defaultPlace = db.monitoredPlaceDao().getDefaultPlace()
                ?: db.monitoredPlaceDao().getAllPlaces().firstOrNull()

            if (defaultPlace != null) {
                val repository = FloodRepository(context)
                val forecastResult = repository.fetchFloodForecast(defaultPlace.latitude, defaultPlace.longitude)

                if (forecastResult.isSuccess) {
                    val days = forecastResult.getOrDefault(emptyList())
                    val alertDays = RiskCalculator.getUpcomingAlerts(days, daysToLookAhead = 7)
                    val notificationHelper = NotificationHelper(context)

                    for (alertDay in alertDays) {
                        notificationHelper.notifyFloodAlert(defaultPlace.name, alertDay)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
