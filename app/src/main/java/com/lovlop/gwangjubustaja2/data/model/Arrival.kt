package com.lovlop.gwangjubustaja2.data.model

data class Arrival(
    val routeId: String,
    val routeName: String,
    val routeType: Int,
    val minutes: Int,
    val rawMinutes: Int,
    val isCorrected: Boolean
) {
    val correctionDelta: Int
        get() = minutes - rawMinutes

    val isArrivingSoon: Boolean
        get() = minutes <= 1
}