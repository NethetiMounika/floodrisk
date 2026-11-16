package com.example.floodrisk.utils

import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.RiskLevel

object RiskCalculator {

    fun calculateRisk(discharge: Double?, mean: Double?): RiskLevel {
        if (discharge == null || mean == null || mean <= 0.0) {
            return RiskLevel.UNAVAILABLE
        }
        val ratio = discharge / mean
        return when {
            ratio <= 1.5 -> RiskLevel.NORMAL
            ratio <= 2.0 -> RiskLevel.WATCH
            else -> RiskLevel.ALERT
        }
    }

    fun calculateRatio(discharge: Double?, mean: Double?): Double? {
        if (discharge == null || mean == null || mean <= 0.0) {
            return null
        }
        return discharge / mean
    }

    fun findPeakDay(forecastDays: List<FloodForecastDay>): FloodForecastDay? {
        if (forecastDays.isEmpty()) return null
        return forecastDays
            .filter { it.riverDischarge != null || it.riverDischargeMax != null }
            .maxByOrNull { it.riverDischargeMax ?: it.riverDischarge ?: -1.0 }
    }

    fun getUpcomingAlerts(forecastDays: List<FloodForecastDay>, daysToLookAhead: Int = 7): List<FloodForecastDay> {
        val next7 = forecastDays.take(daysToLookAhead)
        return next7.filter { it.riskLevel == RiskLevel.ALERT }
    }
}
