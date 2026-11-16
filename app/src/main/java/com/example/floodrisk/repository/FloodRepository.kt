package com.example.floodrisk.repository

import android.content.Context
import com.example.floodrisk.api.RetrofitClient
import com.example.floodrisk.database.AppDatabase
import com.example.floodrisk.location.LocationHelper
import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.MonitoredPlace
import com.example.floodrisk.utils.RiskCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FloodRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val placeDao = database.monitoredPlaceDao()
    private val locationHelper = LocationHelper(context)

    val allMonitoredPlaces: Flow<List<MonitoredPlace>> = placeDao.getAllPlacesFlow()

    suspend fun fetchFloodForecast(latitude: Double, longitude: Double): Result<List<FloodForecastDay>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = RetrofitClient.api.getFloodForecast(latitude, longitude)
            if (response.isSuccessful) {
                val body = response.body()
                val daily = body?.daily
                if (daily?.time != null) {
                    val daysList = mutableListOf<FloodForecastDay>()
                    val times = daily.time
                    val discharges = daily.riverDischarge ?: emptyList()
                    val means = daily.riverDischargeMean ?: emptyList()
                    val maxes = daily.riverDischargeMax ?: emptyList()

                    for (i in times.indices) {
                        val date = times[i]
                        val discharge = discharges.getOrNull(i)
                        val mean = means.getOrNull(i)
                        val max = maxes.getOrNull(i)
                        val ratio = RiskCalculator.calculateRatio(discharge, mean)
                        val riskLevel = RiskCalculator.calculateRisk(discharge, mean)

                        daysList.add(
                            FloodForecastDay(
                                date = date,
                                riverDischarge = discharge,
                                riverDischargeMean = mean,
                                riverDischargeMax = max,
                                ratio = ratio,
                                riskLevel = riskLevel
                            )
                        )
                    }
                    Result.success(daysList)
                } else {
                    Result.failure(Exception("No daily forecast data available for this location."))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentLocation() = locationHelper.getCurrentLocation()

    fun hasLocationPermissions() = locationHelper.hasLocationPermissions()

    suspend fun getAllPlaces(): List<MonitoredPlace> = withContext(Dispatchers.IO) {
        placeDao.getAllPlaces()
    }

    suspend fun getDefaultPlace(): MonitoredPlace? = withContext(Dispatchers.IO) {
        placeDao.getDefaultPlace()
    }

    suspend fun addMonitoredPlace(place: MonitoredPlace): Long = withContext(Dispatchers.IO) {
        val id = placeDao.insertPlace(place)
        val count = placeDao.getAllPlaces().size
        if (count == 1 || place.isDefault) {
            setDefaultPlace(id)
        }
        id
    }

    suspend fun updateMonitoredPlace(place: MonitoredPlace) = withContext(Dispatchers.IO) {
        placeDao.updatePlace(place)
    }

    suspend fun deleteMonitoredPlace(place: MonitoredPlace) = withContext(Dispatchers.IO) {
        placeDao.deletePlace(place)
    }

    suspend fun setDefaultPlace(placeId: Long) = withContext(Dispatchers.IO) {
        placeDao.clearDefaultFlags()
        val place = placeDao.getPlaceById(placeId)
        if (place != null) {
            placeDao.updatePlace(place.copy(isDefault = true))
        }
    }
}
