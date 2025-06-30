package dev.croock.proximity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.croock.proximity.data.PointOfInterestEntity
import dev.croock.proximity.data.ProximityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlacesOfInterestViewModel(
    app: Application,
    private val tripId: Long
) : AndroidViewModel(app) {
    private val repository = ProximityRepository(app)
    val pointsOfInterest: StateFlow<List<PointOfInterestEntity>> =
        repository.getPointsOfInterestForTrip(tripId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePlaceStatus(poi: PointOfInterestEntity, isActive: Boolean) {
        viewModelScope.launch {
            repository.updatePointOfInterest(poi.copy(isActive = isActive))
        }
    }

    fun deletePlace(poi: PointOfInterestEntity) {
        viewModelScope.launch {
            repository.deletePointOfInterest(poi)
        }
    }

    fun addPlace(tripId: Long, googlePlaceId: String, name: String, lat: Double, lon: Double, isActive: Boolean = true) {
        viewModelScope.launch {
            val poi = dev.croock.proximity.PointOfInterest(
                name = name,
                id = "",
                googlePlaceId = googlePlaceId,
                lat = lat,
                lon = lon,
                isActive = isActive
            ).toEntity(tripId)
            repository.insertPointOfInterest(poi)
        }
    }
}

class PlacesOfInterestViewModelFactory(
    private val app: Application,
    private val tripId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlacesOfInterestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlacesOfInterestViewModel(app, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
