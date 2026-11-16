package com.example.floodrisk.model

data class FloodForecastDay(
    val date: String,
    val riverDischarge: Double?,
    val riverDischargeMean: Double?,
    val riverDischargeMax: Double?,
    val ratio: Double?,
    val riskLevel: RiskLevel
)
