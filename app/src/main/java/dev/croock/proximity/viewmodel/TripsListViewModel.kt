package dev.croock.proximity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.croock.proximity.data.ProximityRepository
import dev.croock.proximity.data.TripEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TripsListViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ProximityRepository(app)
    val trips: StateFlow<List<TripEntity>> =
        repository.getAllTrips().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class TripsListViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripsListViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
