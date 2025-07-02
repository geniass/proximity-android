package dev.croock.proximity.util

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class LocationUtilsTest {

    @Test
    fun calculateBearing_fromOriginToNorth_returns0() {
        // Arrange: From equator to 1 degree north
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 1.0
        val lon2 = 0.0
        
        // Act
        val bearing = LocationUtils.calculateBearing(lat1, lon1, lat2, lon2)
        
        // Assert: Should be approximately 0 degrees (north)
        assertEquals(0.0f, bearing, 1.0f)
    }

    @Test
    fun calculateBearing_fromOriginToEast_returns90() {
        // Arrange: From origin to 1 degree east
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 0.0
        val lon2 = 1.0
        
        // Act
        val bearing = LocationUtils.calculateBearing(lat1, lon1, lat2, lon2)
        
        // Assert: Should be approximately 90 degrees (east)
        assertEquals(90.0f, bearing, 1.0f)
    }

    @Test
    fun calculateBearing_fromOriginToSouth_returns180() {
        // Arrange: From origin to 1 degree south
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = -1.0
        val lon2 = 0.0
        
        // Act
        val bearing = LocationUtils.calculateBearing(lat1, lon1, lat2, lon2)
        
        // Assert: Should be approximately 180 degrees (south)
        assertEquals(180.0f, bearing, 1.0f)
    }

    @Test
    fun calculateBearing_fromOriginToWest_returns270() {
        // Arrange: From origin to 1 degree west
        val lat1 = 0.0
        val lon1 = 0.0
        val lat2 = 0.0
        val lon2 = -1.0
        
        // Act
        val bearing = LocationUtils.calculateBearing(lat1, lon1, lat2, lon2)
        
        // Assert: Should be approximately 270 degrees (west)
        assertEquals(270.0f, bearing, 1.0f)
    }

    @Test
    fun calculateBearing_realWorldExample_tokyoToOsaka() {
        // Arrange: From Tokyo (35.6762, 139.6503) to Osaka (34.6937, 135.5023)
        val tokyoLat = 35.6762
        val tokyoLon = 139.6503
        val osakaLat = 34.6937
        val osakaLon = 135.5023
        
        // Act
        val bearing = LocationUtils.calculateBearing(tokyoLat, tokyoLon, osakaLat, osakaLon)
        
        // Assert: Tokyo to Osaka should be roughly southwest (around 240 degrees)
        assertTrue("Bearing should be between 220 and 260 degrees", bearing in 220.0f..260.0f)
    }

    @Test
    fun bearingToDirection_exactCardinalDirections() {
        assertEquals("north", LocationUtils.bearingToDirection(0.0f))
        assertEquals("east", LocationUtils.bearingToDirection(90.0f))
        assertEquals("south", LocationUtils.bearingToDirection(180.0f))
        assertEquals("west", LocationUtils.bearingToDirection(270.0f))
        assertEquals("north", LocationUtils.bearingToDirection(360.0f))
    }

    @Test
    fun bearingToDirection_intercardinalDirections() {
        assertEquals("northeast", LocationUtils.bearingToDirection(45.0f))
        assertEquals("southeast", LocationUtils.bearingToDirection(135.0f))
        assertEquals("southwest", LocationUtils.bearingToDirection(225.0f))
        assertEquals("northwest", LocationUtils.bearingToDirection(315.0f))
    }

    @Test
    fun bearingToDirection_boundaryValues() {
        // Test boundary values for north
        assertEquals("north", LocationUtils.bearingToDirection(22.5f))
        assertEquals("northeast", LocationUtils.bearingToDirection(22.6f))
        assertEquals("north", LocationUtils.bearingToDirection(337.5f))
        assertEquals("northwest", LocationUtils.bearingToDirection(337.4f))
        
        // Test boundary values for east
        assertEquals("northeast", LocationUtils.bearingToDirection(67.5f))
        assertEquals("east", LocationUtils.bearingToDirection(67.6f))
        assertEquals("east", LocationUtils.bearingToDirection(112.5f))
        assertEquals("southeast", LocationUtils.bearingToDirection(112.6f))
    }

    @Test
    fun bearingToDirection_invalidBearing_returnsUnknown() {
        assertEquals("unknown", LocationUtils.bearingToDirection(-1.0f))
        assertEquals("unknown", LocationUtils.bearingToDirection(361.0f))
        assertEquals("unknown", LocationUtils.bearingToDirection(Float.NaN))
    }

    @Test
    fun formatDistance_meters() {
        assertEquals("0m", LocationUtils.formatDistance(0.0f))
        assertEquals("1m", LocationUtils.formatDistance(1.0f))
        assertEquals("50m", LocationUtils.formatDistance(50.3f))
        assertEquals("126m", LocationUtils.formatDistance(125.7f))
        assertEquals("1000m", LocationUtils.formatDistance(999.9f))
    }

    @Test
    fun formatDistance_kilometers() {
        assertEquals("1km", LocationUtils.formatDistance(1000.0f))
        assertEquals("1km", LocationUtils.formatDistance(1200.0f))
        assertEquals("2km", LocationUtils.formatDistance(1500.0f))
        assertEquals("2km", LocationUtils.formatDistance(1999.0f))
        assertEquals("5km", LocationUtils.formatDistance(5000.0f))
        assertEquals("11km", LocationUtils.formatDistance(10500.0f))
    }

    @Test
    fun formatDistance_roundingBehavior() {
        // Test rounding behavior near boundaries
        assertEquals("999m", LocationUtils.formatDistance(999.4f))
        assertEquals("1000m", LocationUtils.formatDistance(999.5f))
        assertEquals("1km", LocationUtils.formatDistance(1000.4f))
        assertEquals("1km", LocationUtils.formatDistance(1499.9f))
        assertEquals("2km", LocationUtils.formatDistance(1500.0f))
    }

    @Test
    fun formatDistance_largeDistances() {
        assertEquals("100km", LocationUtils.formatDistance(100000.0f))
        assertEquals("1000km", LocationUtils.formatDistance(1000000.0f))
    }
}