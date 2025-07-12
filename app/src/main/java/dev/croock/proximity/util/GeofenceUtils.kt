package dev.croock.proximity.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dev.croock.proximity.receiver.GeofenceTransitionReceiver

object GeofenceUtils {
    private const val GEOFENCE_RADIUS_METERS = 200f
    private const val GEOFENCE_REQ_ID = "USER_DYNAMIC_GEOFENCE"

    fun replaceGeofence(context: Context, lat: Double, lng: Double) {
        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_REQ_ID)
            .setCircularRegion(lat, lng, GEOFENCE_RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
//            This line supposedly improves battery life by delaying the notification, but lets see
//            .setNotificationResponsiveness(2.minutes.inWholeMilliseconds.toInt())
            .build()
        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        val intent = Intent(context, GeofenceTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            geofencingClient.removeGeofences(pendingIntent).addOnCompleteListener {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
