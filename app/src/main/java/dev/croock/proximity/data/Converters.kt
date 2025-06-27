package dev.croock.proximity.data

import androidx.room.TypeConverter
import java.time.LocalDate

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    @JvmStatic
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }
}
