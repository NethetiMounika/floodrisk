package com.example.floodrisk.api

import com.example.floodrisk.model.FloodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoFloodApi {
    @GET("v1/flood")
    suspend fun getFloodForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "river_discharge,river_discharge_mean,river_discharge_max",
        @Query("forecast_days") forecastDays: Int = 30
    ): Response<FloodResponse>
}
