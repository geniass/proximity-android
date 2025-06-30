package dev.croock.proximity

import android.app.Application
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitFetchPlace
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import dev.croock.proximity.ui.theme.ProximityTheme
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModel
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModelFactory
import dev.croock.proximity.viewmodel.toDomain
import dev.croock.proximity.viewmodel.toEntity
import kotlinx.coroutines.launch

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
    onOpenInMaps: (PointOfInterest) -> Unit,
    onDeletePlace: (PointOfInterest) -> Unit,
    onTogglePlaceStatus: (PointOfInterest, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PlacesOfInterestViewModel = viewModel(
        factory = PlacesOfInterestViewModelFactory(context, tripId)
    )
    val poiEntities by viewModel.pointsOfInterest.collectAsState()
    val places = poiEntities.map { it.toDomain() }

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
                val sessionToken: AutocompleteSessionToken? = PlaceAutocomplete.getSessionTokenFromIntent(intent)
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
                    Log.i(TAG, "Adding Place: ${place.displayName} (${placeResult.placeId}), $latLng")
                    viewModel.addPlace(
                        tripId = tripId,
                        googlePlaceId = placeResult.placeId,
                        name = place.displayName!!,
                        lat = place.location!!.latitude,
                        lon = place.location!!.longitude
                    )
                    Toast.makeText(context, "Added place of interest \"${place.displayName}\"", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = { /* TODO: Implement view toggle (list/map) */ }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Toggle Map View")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launchPlacesAutocomplete() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Place of Interest")
            }
        }
    ) { innerPadding ->
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
                    onOpenInMaps = { onOpenInMaps(place) },
                    onDeletePlace = { viewModel.deletePlace(place.toEntity(tripId)) },
                    onToggleStatus = { newStatus ->
                        viewModel.togglePlaceStatus(place.toEntity(tripId), newStatus)
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceOfInterestCard(
    place: PointOfInterest,
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
            onOpenInMaps = {},
            onDeletePlace = {},
            onTogglePlaceStatus = { _, _ -> }
        )
    }
}
