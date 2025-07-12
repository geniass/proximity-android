package dev.croock.proximity.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.croock.proximity.data.ProximityDatabase
import dev.croock.proximity.util.GeofenceUtils
import dev.croock.proximity.util.NotificationUtils.showNotification
import dev.croock.proximity.util.PlaceNotificationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GeofenceTransitionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error: ${geofencingEvent.errorCode}")
            return
        }
        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Geofence EXIT detected")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        location?.let { currentLocation ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = ProximityDatabase.getDatabase(context)
                                val trips = db.tripDao().getAllTrips().firstOrNull() ?: emptyList()
                                val allClosePlaces = mutableListOf<PlaceNotificationInfo>()
                                trips.forEach { trip ->
                                    val places = db.pointOfInterestDao().getPointsOfInterestForTrip(trip.id).firstOrNull() ?: emptyList()
                                    val closePlaces = places.filter { poi ->
                                        val result = FloatArray(1)
                                        Location.distanceBetween(
                                            currentLocation.latitude, currentLocation.longitude,
                                            poi.lat, poi.lon, result
                                        )
                                        result[0] <= 500
                                    }
                                    closePlaces.forEach { poi ->
                                        Log.i(TAG, "Place within 500m: ${'$'}{poi.name} at ${'$'}{poi.lat},${'$'}{poi.lon} (Trip: ${'$'}{trip.name})")
                                        allClosePlaces.add(
                                            PlaceNotificationInfo(
                                                name = poi.name,
                                                tripName = trip.name,
                                                lat = poi.lat,
                                                lon = poi.lon
                                            )
                                        )
                                    }
                                }
                                if (allClosePlaces.isNotEmpty()) {
                                    showNotification(context, allClosePlaces, currentLocation)
                                }
                                // Re-create geofence at new location
                                GeofenceUtils.replaceGeofence(context, currentLocation.latitude, currentLocation.longitude)
                            }
                        }
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission not granted", e)
            }
        }
    }
}
