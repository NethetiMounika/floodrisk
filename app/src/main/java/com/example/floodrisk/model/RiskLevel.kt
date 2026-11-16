package com.example.floodrisk.model

enum class RiskLevel(val displayName: String) {
    NORMAL("Normal"),
    WATCH("Watch"),
    ALERT("Alert"),
    UNAVAILABLE("Unavailable")
}
