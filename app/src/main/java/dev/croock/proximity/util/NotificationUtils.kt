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
    private const val STANDALONE_SUMMARY_CHANNEL_ID = "geofence_places_standalone_summary_channel"
    private const val GROUP_KEY = "proximity_places_group"
    private const val SUMMARY_NOTIFICATION_ID = 1000
    private const val STANDALONE_SUMMARY_NOTIFICATION_ID = 2000

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
        notificationManager.cancel(STANDALONE_SUMMARY_NOTIFICATION_ID)
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

            // Create standalone summary notification for single place too
            val standaloneSummaryNotification = NotificationCompat.Builder(context, STANDALONE_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(distanceInfo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(STANDALONE_SUMMARY_NOTIFICATION_ID, standaloneSummaryNotification)
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
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)

            // Create standalone summary notification (not part of group)
            val standaloneSummaryNotification = NotificationCompat.Builder(context, STANDALONE_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${places.size} nearby places")
                .setContentText("Tap to view all")
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(STANDALONE_SUMMARY_NOTIFICATION_ID, standaloneSummaryNotification)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // Main notification channel - for grouped notifications
            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                "Nearby Places (Grouped)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Grouped notifications for nearby places of interest"
                enableVibration(true)
            }

            // Standalone summary channel - for independent summary notification
            val standaloneSummaryChannel = NotificationChannel(
                STANDALONE_SUMMARY_CHANNEL_ID,
                "Nearby Places (Summary Only)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standalone summary notification that can be kept while muting grouped notifications"
                enableVibration(true)

            }

            manager.createNotificationChannel(mainChannel)
            manager.createNotificationChannel(standaloneSummaryChannel)
        }
    }
}