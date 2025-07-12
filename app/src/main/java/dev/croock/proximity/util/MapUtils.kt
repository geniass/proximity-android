package dev.croock.proximity.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object MapUtils {

    fun launchGoogleMaps(context: Context, name: String, googlePlaceId: String) {
        val query = Uri.encode(name)
        val gmmIntentUri = "https://www.google.com/maps/search/?api=1&query=$query&query_place_id=${googlePlaceId}".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(mapIntent)
    }
    
    fun googleMapsDirectionsPendingIntent(context: Context, name: String, googlePlaceId: String): PendingIntent {
        val mapIntent = googleMapsDirectionsIntent(name, googlePlaceId)
        return PendingIntent.getActivity(context, 0, mapIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun googleMapsDirectionsIntent(name: String, googlePlaceId: String): Intent {
        val destination = Uri.encode(name)
        val gmmIntentUri = "https://www.google.com/maps/dir/?api=1&destination=$destination&destination_place_id=$googlePlaceId".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return mapIntent
    }
}
