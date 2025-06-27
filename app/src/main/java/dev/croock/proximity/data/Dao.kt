package dev.croock.proximity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)
}

@Dao
interface PointOfInterestDao {
    @Query("SELECT * FROM points_of_interest WHERE trip_id = :tripId")
    fun getPointsOfInterestForTrip(tripId: Long): Flow<List<PointOfInterestEntity>>

    @Insert
    suspend fun insertPointOfInterest(poi: PointOfInterestEntity): Long

    @Update
    suspend fun updatePointOfInterest(poi: PointOfInterestEntity)

    @Delete
    suspend fun deletePointOfInterest(poi: PointOfInterestEntity)
}
