package dev.croock.proximity

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.croock.proximity.ui.theme.ProximityTheme
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModel
import dev.croock.proximity.viewmodel.PlacesOfInterestViewModelFactory
import dev.croock.proximity.viewmodel.toDomain
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.croock.proximity.viewmodel.toEntity

// Data class for Point of Interest
data class PointOfInterest(
    val id: String,
    val name: String,
    val address: String,
    var isActive: Boolean
)

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
    val context = LocalContext.current.applicationContext as Application
    val viewModel: PlacesOfInterestViewModel = viewModel(
        factory = PlacesOfInterestViewModelFactory(context, tripId)
    )
    val poiEntities by viewModel.pointsOfInterest.collectAsState()
    val places = poiEntities.map { it.toDomain() }

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
            Text(
                text = place.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
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
                    Icon(Icons.Default.Place, contentDescription = "Open in Maps", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN IN MAPS", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onDeletePlace) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Place", tint = MaterialTheme.colorScheme.error)
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
