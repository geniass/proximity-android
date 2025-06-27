package dev.croock.proximity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.croock.proximity.ui.theme.ProximityTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.croock.proximity.viewmodel.TripsListViewModel
import dev.croock.proximity.viewmodel.TripsListViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import dev.croock.proximity.data.TripEntity

// Define navigation routes
object NavRoutes {
    const val TRIPS_LIST = "tripsList"
    const val PLACES_OF_INTEREST = "placesOfInterest/{tripId}/{tripName}"

    fun placesOfInterestRoute(tripId: Long, tripName: String) = "placesOfInterest/$tripId/$tripName"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProximityTheme {
                val navController = rememberNavController()
                AppNavigator(navController = navController)
            }
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
            PlacesOfInterestScreen(
                tripId = tripId,
                tripName = tripName,
                onNavigateBack = { navController.popBackStack() },
                onOpenInMaps = { /* TODO */ },
                onDeletePlace = { /* TODO */ },
                onTogglePlaceStatus = { _, _ -> /* TODO */ }
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

// Minimal Trip data class
data class Trip(
    val name: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) {
    val dateRangeString: String
        get() = if (startDate != null && endDate != null) {
            val formatter = DateTimeFormatter.ofPattern("E, d MMMM yyyy", Locale.ENGLISH)
            val startStr = startDate.format(formatter)
            val endStr = endDate.format(formatter)
            "$startStr - $endStr"
        } else ""

    companion object {
        fun create(name: String, startDate: LocalDate?, endDate: LocalDate?): Trip {
            return Trip(name, startDate, endDate)
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