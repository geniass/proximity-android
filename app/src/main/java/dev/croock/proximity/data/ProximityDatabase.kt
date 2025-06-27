package dev.croock.proximity.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [TripEntity::class, PointOfInterestEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ProximityDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun pointOfInterestDao(): PointOfInterestDao

    companion object {
        @Volatile
        private var INSTANCE: ProximityDatabase? = null

        fun getDatabase(context: Context): ProximityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProximityDatabase::class.java,
                    "proximity_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed DB in IO thread
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            val tripDao = database.tripDao()
                            if (tripDao.getAllTrips().firstOrNull().isNullOrEmpty()) {
                                tripDao.insertTrip(TripEntity(name = "Tokyo", startDate = LocalDate.of(2025, 4, 1), endDate = LocalDate.of(2025, 4, 30)))
                                tripDao.insertTrip(TripEntity(name = "Joburg", startDate = null, endDate = null))
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
