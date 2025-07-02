package dev.croock.proximity.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.croock.proximity.R

data class PlaceNotificationInfo(
    val name: String,
    val tripName: String,
    val lat: Double,
    val lon: Double
)

object NotificationUtils {
    private const val CHANNEL_ID = "geofence_places_channel"
    private const val GROUP_KEY = "proximity_places_group"
    private const val SUMMARY_NOTIFICATION_ID = 1000
    
    fun showNotification(
        context: Context, 
        places: List<PlaceNotificationInfo>,
        userLocation: Location
    ) {
        createNotificationChannel(context)
        
        if (places.isEmpty()) return
        
        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        // Cancel existing notifications
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        places.forEachIndexed { index, _ ->
            notificationManager.cancel(SUMMARY_NOTIFICATION_ID + index + 1)
        }
        
        if (places.size == 1) {
            // Single notification
            val place = places.first()
            val result = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                place.lat, place.lon, result
            )
            val bearing = LocationUtils.calculateBearing(userLocation.latitude, userLocation.longitude, place.lat, place.lon)
            val direction = LocationUtils.bearingToDirection(bearing)
            val distanceInfo = "${LocationUtils.formatDistance(result[0])} $direction"
            
            val title = if (place.tripName.isNotEmpty()) "${place.name} (${place.tripName})" else place.name
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(distanceInfo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, notification)
        } else {
            // Multiple notifications - create group
            places.forEachIndexed { index, place ->
                val result = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    place.lat, place.lon, result
                )
                val bearing = LocationUtils.calculateBearing(userLocation.latitude, userLocation.longitude, place.lat, place.lon)
                val direction = LocationUtils.bearingToDirection(bearing)
                val distanceInfo = "${LocationUtils.formatDistance(result[0])} $direction"
                
                val title = if (place.tripName.isNotEmpty()) "${place.name} (${place.tripName})" else place.name
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(distanceInfo)
                    .setGroup(GROUP_KEY)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                
                notificationManager.notify(SUMMARY_NOTIFICATION_ID + index + 1, notification)
            }
            
            // Create summary notification
            val summaryText = places.joinToString("\n") { place ->
                val result = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    place.lat, place.lon, result
                )
                val bearing = LocationUtils.calculateBearing(userLocation.latitude, userLocation.longitude, place.lat, place.lon)
                val direction = LocationUtils.bearingToDirection(bearing)
                val distanceInfo = "${LocationUtils.formatDistance(result[0])} $direction"
                
                val title = if (place.tripName.isNotEmpty()) "${place.name} (${place.tripName})" else place.name
                "$title • $distanceInfo"
            }
            
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${places.size} nearby places")
                .setContentText("Tap to view all")
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nearby Places",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for nearby places of interest"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}