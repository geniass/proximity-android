package dev.croock.proximity.data

import android.app.Application
import kotlinx.coroutines.flow.Flow

class ProximityRepository(app: Application) {
    private val db = ProximityDatabase.getDatabase(app)
    private val tripDao = db.tripDao()
    private val poiDao = db.pointOfInterestDao()

    fun getAllTrips(): Flow<List<TripEntity>> = tripDao.getAllTrips()
    fun getPointsOfInterestForTrip(tripId: Long, onlyActive: Boolean = false): Flow<List<PointOfInterestEntity>> =
        poiDao.getPointsOfInterestForTrip(tripId, onlyActive)

    suspend fun insertTrip(trip: TripEntity): Long = tripDao.insertTrip(trip)
    suspend fun insertPointOfInterest(poi: PointOfInterestEntity): Long = poiDao.insertPointOfInterest(poi)
    suspend fun updatePointOfInterest(poi: PointOfInterestEntity) = poiDao.updatePointOfInterest(poi)
    suspend fun deletePointOfInterest(poi: PointOfInterestEntity) = poiDao.deletePointOfInterest(poi)
}
