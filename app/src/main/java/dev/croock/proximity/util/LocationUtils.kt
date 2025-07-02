package dev.croock.proximity.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

object LocationUtils {
    
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        
        val bearingRad = atan2(y, x)
        return ((Math.toDegrees(bearingRad) + 360) % 360).toFloat()
    }
    
    fun bearingToDirection(bearing: Float): String {
        return when (bearing) {
            in 0f..22.5f, in 337.5f..360f -> "north"
            in 22.5f..67.5f -> "northeast"
            in 67.5f..112.5f -> "east"
            in 112.5f..157.5f -> "southeast"
            in 157.5f..202.5f -> "south"
            in 202.5f..247.5f -> "southwest"
            in 247.5f..292.5f -> "west"
            in 292.5f..337.5f -> "northwest"
            else -> "unknown"
        }
    }
    
    fun formatDistance(distanceMeters: Float): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.roundToInt()}m"
            else -> "${(distanceMeters / 1000).roundToInt()}km"
        }
    }
}