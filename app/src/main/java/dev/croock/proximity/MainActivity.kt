package dev.croock.proximity

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.croock.proximity.data.ProximityDatabase
import dev.croock.proximity.data.TripEntity
import dev.croock.proximity.ui.theme.ProximityTheme
import dev.croock.proximity.util.GeofenceUtils
import dev.croock.proximity.util.NotificationUtils.showNotification
import dev.croock.proximity.util.PlaceNotificationInfo
import dev.croock.proximity.viewmodel.TripsListViewModel
import dev.croock.proximity.viewmodel.TripsListViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Define navigation routes
object NavRoutes {
    const val TRIPS_LIST = "tripsList"
    const val PLACES_OF_INTEREST = "placesOfInterest/{tripId}/{tripName}?showMap={showMap}"

    fun placesOfInterestRoute(tripId: Long, tripName: String, showMap: Boolean = false) = "placesOfInterest/$tripId/$tripName?showMap=$showMap"
}

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var permissionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.i(TAG, "Permission not granted: ${REQUIRED_PERMISSIONS[permissionIndex]}")
            }
            permissionIndex++
            requestNextPermission()
        }
        enableEdgeToEdge()
        setContent {
            var showRationale by remember { mutableStateOf(false) }
            var rationaleText by remember { mutableStateOf("") }
            ProximityTheme {
                val navController = rememberNavController()
                AppNavigator(navController = navController)

                LaunchedEffect(Unit) {
                    val placeInfo = intent.getParcelableExtra(
                        Constants.EXTRA_PLACE_INFO,
                        PlaceNotificationInfo::class.java
                    )

                    if (placeInfo != null) {
                        navController.navigate(
                            NavRoutes.placesOfInterestRoute(
                                tripId = placeInfo.tripId,
                                tripName = placeInfo.tripName,
                                showMap = true,
                            )
                        )
                    } else if (intent.getBooleanExtra(Constants.EXTRA_SHOW_MAP, false)) {
                        val tripId = intent.getLongExtra(Constants.EXTRA_TRIP_ID, 0L)
                        val tripName = intent.getStringExtra(Constants.EXTRA_TRIP_NAME) ?: "Unknown Trip"
                        navController.navigate(NavRoutes.placesOfInterestRoute(tripId, tripName, true))
                    }
                }

                if (showRationale) {
                    AlertDialog(
                        onDismissRequest = { showRationale = false },
                        title = { Text("Permission Required") },
                        text = { Text(rationaleText) },
                        confirmButton = {
                            Button(onClick = {
                                showRationale = false
                                permissionLauncher.launch(REQUIRED_PERMISSIONS[permissionIndex])
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRationale = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
        requestNextPermission()
        getCurrentLocationAndCompare()
    }

    private fun requestNextPermission() {
        if (permissionIndex >= REQUIRED_PERMISSIONS.size) return
        val permission = REQUIRED_PERMISSIONS[permissionIndex]
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                permissionIndex++
                requestNextPermission()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                runOnUiThread {
                    // Show rationale dialog in Compose
                    val rationale = when (permission) {
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ->
                            "Location permission is required for geofencing and proximity features."

                        Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
                            "Background location permission is required for geofencing to work when the app is not in use."

                        Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
                            "Notification permission is required to alert you about nearby places."

                        else -> "This permission is required for the app to function."
                    }
                    // Use Compose state to show dialog
                    setContent {
                        var showRationale by remember { mutableStateOf(true) }
                        var rationaleText by remember { mutableStateOf(rationale) }
                        ProximityTheme {
                            val navController = rememberNavController()
                            AppNavigator(navController = navController)
                            if (showRationale) {
                                AlertDialog(
                                    onDismissRequest = { showRationale = false },
                                    title = { Text("Permission Required") },
                                    text = { Text(rationaleText) },
                                    confirmButton = {
                                        Button(onClick = {
                                            showRationale = false
                                            permissionLauncher.launch(permission)
                                        }) { Text("OK") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRationale = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun getCurrentLocationAndCompare() {
        Log.d(TAG, "getCurrentLocationAndCompare")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    location?.let { currentLocation ->
                        Log.d(TAG, "Current location: $currentLocation")
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = ProximityDatabase.getDatabase(this@MainActivity)
                            val trips = db.tripDao().getAllTrips().firstOrNull() ?: emptyList()
                            val allClosePlaces = mutableListOf<PlaceNotificationInfo>()
                            trips.forEach { trip ->
                                val places = db.pointOfInterestDao().getPointsOfInterestForTrip(trip.id, true).firstOrNull() ?: emptyList()
                                val closePlaces = places.filter { poi ->
                                    val result = FloatArray(1)
                                    Location.distanceBetween(
                                        currentLocation.latitude, currentLocation.longitude,
                                        poi.lat, poi.lon, result
                                    )
                                    result[0] <= 500
                                }
                                closePlaces.forEach { poi ->
                                    Log.i(TAG, "Place within 500m: ${poi.name} at ${poi.lat},${poi.lon} (Trip: ${trip.name})")
                                    allClosePlaces.add(
                                        PlaceNotificationInfo(
                                            name = poi.name,
                                            tripId = trip.id,
                                            tripName = trip.name,
                                            lat = poi.lat,
                                            lon = poi.lon,
                                            googlePlaceId = poi.googlePlaceId
                                        )
                                    )
                                }
                            }
                            if (allClosePlaces.isNotEmpty()) {
                                showNotification(this@MainActivity, allClosePlaces, currentLocation)
                            }
                            // Re-create geofence at new location
                            GeofenceUtils.replaceGeofence(this@MainActivity, currentLocation.latitude, currentLocation.longitude)
                        }
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }
}

@Composable
fun AppNavigator(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.TRIPS_LIST) {
        composable(NavRoutes.TRIPS_LIST) {
            TripsListScreen(
                onTripClick = { trip ->
                    navController.navigate(NavRoutes.placesOfInterestRoute(trip.id, trip.name))
                }
            )
        }
        composable(NavRoutes.PLACES_OF_INTEREST) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")?.toLongOrNull() ?: 0L
            val tripName = backStackEntry.arguments?.getString("tripName") ?: "Trip Details"
            val showMap = backStackEntry.arguments?.getString("showMap")?.toBoolean() ?: false
            PlacesOfInterestScreen(
                tripId = tripId,
                tripName = tripName,
                onNavigateBack = { navController.popBackStack() },
                showMap = showMap,
            )
        }
    }
}

@Composable
private fun TripProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 3.dp
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TripsListScreen(onTripClick: (TripEntity) -> Unit) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: TripsListViewModel = viewModel(factory = TripsListViewModelFactory(context))
    val trips by viewModel.trips.collectAsState()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Add trip */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top
        ) {
            items(trips) { trip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onTripClick(trip) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trip.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.size(4.dp))
                            val dateRangeString = if (trip.startDate != null && trip.endDate != null) "${trip.startDate} - ${trip.endDate}" else ""
                            if (dateRangeString.isNotBlank()) {
                                Text(dateRangeString, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Calculate and display progress
                        // Using LocalDate.now() here for simplicity. For performance in very long lists,
                        // it could be hoisted or passed as a parameter.
                        val progressValue = calculateTripProgress(trip.startDate, trip.endDate, LocalDate.now())

                        if (progressValue != null) {
                            Spacer(modifier = Modifier.width(16.dp)) // Space between text and progress indicator
                            TripProgressIndicator(progress = progressValue)
                        }
                    }
                }
            }
        }
    }
}

// Calculates trip progress from 0.0f to 1.0f
// Returns null if dates are invalid or not provided.
private fun calculateTripProgress(
    startDate: LocalDate?,
    endDate: LocalDate?,
    currentDate: LocalDate = LocalDate.now()
): Float? {
    if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
        return null // Invalid input
    }

    if (currentDate.isBefore(startDate)) {
        return 0.0f // Trip hasn't started
    }

    if (!currentDate.isBefore(endDate)) { // Equivalent to currentDate >= endDate
        return 1.0f // Trip is 100% progressed or completed
    }

    // At this point: startDate <= currentDate < endDate, and startDate < endDate.
    // So, totalTripSpanInDays will be > 0.
    val totalTripSpanInDays = ChronoUnit.DAYS.between(startDate, endDate)
    val elapsedDaysSinceStart = ChronoUnit.DAYS.between(startDate, currentDate)

    if (totalTripSpanInDays == 0L) { // Should not be reached if startDate < endDate
        return if (currentDate.isEqual(startDate)) 1.0f else 0.0f // Safety for single day trip if logic above failed
    }

    return (elapsedDaysSinceStart.toFloat() / totalTripSpanInDays.toFloat()).coerceIn(0.0f, 1.0f)
}

@Preview(showBackground = true)
@Composable
fun TripsListScreenPreview() {
    ProximityTheme {
        TripsListScreen(onTripClick = {})
    }
}