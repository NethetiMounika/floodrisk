package com.example.floodrisk.utils

import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RiskCalculatorTest {

    @Test
    fun testRiskLevel_ExactlyOnePointFiveTimesMean_IsNormal() {
        // ratio = 15.0 / 10.0 = 1.5 -> NORMAL
        assertEquals(RiskLevel.NORMAL, RiskCalculator.calculateRisk(15.0, 10.0))
    }

    @Test
    fun testRiskLevel_GreaterThanOnePointFiveTimesMean_IsWatch() {
        // ratio = 15.01 / 10.0 = 1.501 -> WATCH
        assertEquals(RiskLevel.WATCH, RiskCalculator.calculateRisk(15.01, 10.0))
    }

    @Test
    fun testRiskLevel_ExactlyTwoTimesMean_IsWatch() {
        // ratio = 20.0 / 10.0 = 2.0 -> WATCH
        assertEquals(RiskLevel.WATCH, RiskCalculator.calculateRisk(20.0, 10.0))
    }

    @Test
    fun testRiskLevel_GreaterThanTwoTimesMean_IsAlert() {
        // ratio = 20.01 / 10.0 = 2.001 -> ALERT
        assertEquals(RiskLevel.ALERT, RiskCalculator.calculateRisk(20.01, 10.0))
    }

    @Test
    fun testRiskLevel_ZeroMean_IsUnavailable() {
        assertEquals(RiskLevel.UNAVAILABLE, RiskCalculator.calculateRisk(10.0, 0.0))
    }

    @Test
    fun testRiskLevel_NullMean_IsUnavailable() {
        assertEquals(RiskLevel.UNAVAILABLE, RiskCalculator.calculateRisk(10.0, null))
    }

    @Test
    fun testRiskLevel_MissingDischarge_IsUnavailable() {
        assertEquals(RiskLevel.UNAVAILABLE, RiskCalculator.calculateRisk(null, 10.0))
        assertEquals(RiskLevel.UNAVAILABLE, RiskCalculator.calculateRisk(null, null))
    }

    @Test
    fun testFindPeakDay_CorrectHighestDischargeDay() {
        val days = listOf(
            FloodForecastDay("2025-03-01", 10.0, 8.0, 12.0, 1.25, RiskLevel.NORMAL),
            FloodForecastDay("2025-03-02", 45.0, 8.0, 50.0, 5.62, RiskLevel.ALERT),
            FloodForecastDay("2025-03-03", 20.0, 8.0, 22.0, 2.50, RiskLevel.ALERT)
        )
        val peak = RiskCalculator.findPeakDay(days)
        assertEquals("2025-03-02", peak?.date)
        assertEquals(50.0, peak?.riverDischargeMax)
    }

    @Test
    fun testFindPeakDay_MissingValuesAreIgnored() {
        val days = listOf(
            FloodForecastDay("2025-03-01", null, null, null, null, RiskLevel.UNAVAILABLE),
            FloodForecastDay("2025-03-02", 18.0, 10.0, 25.0, 1.8, RiskLevel.WATCH),
            FloodForecastDay("2025-03-03", null, 10.0, null, null, RiskLevel.UNAVAILABLE)
        )
        val peak = RiskCalculator.findPeakDay(days)
        assertEquals("2025-03-02", peak?.date)
    }

    @Test
    fun testFindPeakDay_EmptyForecastIsHandled() {
        assertNull(RiskCalculator.findPeakDay(emptyList()))
    }

    @Test
    fun testUpcomingAlerts_DetectsAlertsWithinSevenDaysOnly() {
        val days = mutableListOf<FloodForecastDay>()
        for (i in 1..5) {
            days.add(FloodForecastDay("2025-03-0$i", 10.0, 10.0, 10.0, 1.0, RiskLevel.NORMAL))
        }
        days.add(FloodForecastDay("2025-03-06", 30.0, 10.0, 30.0, 3.0, RiskLevel.ALERT))
        days.add(FloodForecastDay("2025-03-07", 10.0, 10.0, 10.0, 1.0, RiskLevel.NORMAL))
        days.add(FloodForecastDay("2025-03-10", 30.0, 10.0, 30.0, 3.0, RiskLevel.ALERT))

        val alertsWithin7Days = RiskCalculator.getUpcomingAlerts(days, 7)
        assertEquals(1, alertsWithin7Days.size)
        assertEquals("2025-03-06", alertsWithin7Days[0].date)
    }

    @Test
    fun testUpcomingAlerts_MultipleAlertDaysWithinSevenDays() {
        val days = listOf(
            FloodForecastDay("2025-03-01", 30.0, 10.0, 30.0, 3.0, RiskLevel.ALERT),
            FloodForecastDay("2025-03-02", 25.0, 10.0, 25.0, 2.5, RiskLevel.ALERT),
            FloodForecastDay("2025-03-03", 10.0, 10.0, 10.0, 1.0, RiskLevel.NORMAL)
        )
        val alerts = RiskCalculator.getUpcomingAlerts(days, 7)
        assertEquals(2, alerts.size)
    }
}
