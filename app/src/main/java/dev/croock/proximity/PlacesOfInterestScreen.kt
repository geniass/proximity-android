package dev.croock.proximity

import android.app.Application
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitFetchPlace
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import dev.croock.proximity.ui.theme.ProximityTheme
import dev.croock.proximity.util.MapUtils
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModel
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModelFactory
import dev.croock.proximity.viewmodel.toDomain
import dev.croock.proximity.viewmodel.toEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat

// Data class for Point of Interest
data class PointOfInterest(
    val id: String,
    val name: String,
    val googlePlaceId: String,
    var isActive: Boolean,
    val lat: Double,
    val lon: Double
)

private const val TAG = "PlacesOfInterestScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesOfInterestScreen(
    tripId: Long,
    tripName: String,
    onNavigateBack: () -> Unit,
    showMap: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: PlacesOfInterestViewModel = viewModel(
        factory = PlacesOfInterestViewModelFactory(application, tripId)
    )
    val poiEntities by viewModel.pointsOfInterest.collectAsState()
    val places = poiEntities.map { it.toDomain() }
    var showMapView by remember { mutableStateOf(showMap) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f)
    }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                scope.launch {
                    try {
                        val location = fusedLocationClient.lastLocation.await()
                        if (location != null) {
                            currentLocation = location
                            val userLocation = LatLng(location.latitude, location.longitude)
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(userLocation, 15f)
                                )
                            )
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied", e)
                    }
                }
            }
        }
    )

    LaunchedEffect(showMapView) {
        if (showMapView) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (!Places.isInitialized()) {
        // Define a variable to hold the Places API key.
        val apiKey = BuildConfig.PLACES_API_KEY

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty()) {
            Log.e(TAG, "No places api key")
            return
        }

        // Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(context, apiKey)
    }

    // Google Places Autocomplete launcher
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == PlaceAutocompleteActivity.RESULT_OK && result.data != null) {
                val intent = result.data!!
                val placeResult = PlaceAutocomplete.getPredictionFromIntent(intent)
                if (placeResult == null) {
                    Log.e(TAG, "No place returned")
                    Toast.makeText(context, "No place returned", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
                val sessionToken: AutocompleteSessionToken? =
                    PlaceAutocomplete.getSessionTokenFromIntent(intent)
                val placesClient: PlacesClient = Places.createClient(context)

                scope.launch {
                    val response = placesClient.awaitFetchPlace(
                        placeResult.placeId,
                        listOf(Place.Field.DISPLAY_NAME, Place.Field.LOCATION)
                    ) {
                        sessionToken
                    }
                    val place = response.place
                    val latLng = place.location
                    Log.i(
                        TAG,
                        "Adding Place: ${place.displayName} (${placeResult.placeId}), $latLng"
                    )
                    viewModel.addPlace(
                        tripId = tripId,
                        googlePlaceId = placeResult.placeId,
                        name = place.displayName!!,
                        lat = place.location!!.latitude,
                        lon = place.location!!.longitude
                    )
                    Toast.makeText(
                        context,
                        "Added place of interest \"${place.displayName}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    fun launchPlacesAutocomplete() {
        val sessionToken = AutocompleteSessionToken.newInstance()
        val intent = PlaceAutocomplete.createIntent(context) {
            setAutocompleteSessionToken(sessionToken)
        }
        launcher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tripName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Implement search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Places")
                    }
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            if (showMapView) Icons.AutoMirrored.Filled.List else Icons.Default.Place,
                            contentDescription = "Toggle Map View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!showMapView) {
                FloatingActionButton(onClick = { launchPlacesAutocomplete() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Place of Interest")
                }
            }
        }
    ) { innerPadding ->
        if (showMapView) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                currentLocation?.let {
                    Circle(
                        center = LatLng(it.latitude, it.longitude),
                        radius = 500.0,
                        strokeColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
                places.forEach { place ->
                    val distance = currentLocation?.let {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            it.latitude,
                            it.longitude,
                            place.lat,
                            place.lon,
                            results
                        )
                        results[0]
                    }
                    Marker(
                        state = rememberUpdatedMarkerState(position = LatLng(place.lat, place.lon)),
                        title = place.name,
                        snippet = distance?.let { "%.2f".format(it / 1000) + " km" }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(places, key = { it.id }) { place ->
                    PlaceOfInterestCard(
                        place = place,
                        currentLocation = currentLocation,
                        onOpenInMaps = { MapUtils.launchGoogleMaps(context, place.name, place.googlePlaceId) },
                        onDeletePlace = { viewModel.deletePlace(place.toEntity(tripId)) },
                        onToggleStatus = { newStatus ->
                            viewModel.togglePlaceStatus(place.toEntity(tripId), newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceOfInterestCard(
    place: PointOfInterest,
    currentLocation: Location?,
    onOpenInMaps: () -> Unit,
    onDeletePlace: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            currentLocation?.let {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    it.latitude,
                    it.longitude,
                    place.lat,
                    place.lon,
                    distance
                )
                val df = DecimalFormat("#.##")
                Text(
                    text = "Distance: ${df.format(distance[0] / 1000)} km",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (place.isActive) "Active" else "Ignored",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (place.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = place.isActive,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround // Or Arrangement.End with Spacers
            ) {
                TextButton(onClick = onOpenInMaps) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Open in Maps",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN IN MAPS", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onDeletePlace) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Place",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlacesOfInterestScreenPreview() {
    ProximityTheme {
        PlacesOfInterestScreen(
            tripId = 1L,
            tripName = "Tokyo",
            onNavigateBack = {},
        )
    }
}