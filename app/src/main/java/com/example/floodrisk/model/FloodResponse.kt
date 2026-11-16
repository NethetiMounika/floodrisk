package com.example.floodrisk.model

import com.google.gson.annotations.SerializedName

data class FloodResponse(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("generationtime_ms") val generationTimeMs: Double?,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String?,
    @SerializedName("daily_units") val dailyUnits: DailyUnits?,
    @SerializedName("daily") val daily: DailyData?
)

data class DailyUnits(
    @SerializedName("time") val time: String?,
    @SerializedName("river_discharge") val riverDischarge: String?,
    @SerializedName("river_discharge_mean") val riverDischargeMean: String?,
    @SerializedName("river_discharge_max") val riverDischargeMax: String?
)

data class DailyData(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("river_discharge") val riverDischarge: List<Double?>?,
    @SerializedName("river_discharge_mean") val riverDischargeMean: List<Double?>?,
    @SerializedName("river_discharge_max") val riverDischargeMax: List<Double?>?
)
