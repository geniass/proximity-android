package dev.croock.proximity.viewmodel

import dev.croock.proximity.data.PointOfInterestEntity
import dev.croock.proximity.PointOfInterest

fun PointOfInterestEntity.toDomain(): PointOfInterest = PointOfInterest(
    id = id.toString(),
    name = name,
    address = address,
    isActive = isActive
)

fun PointOfInterest.toEntity(tripId: Long): PointOfInterestEntity = PointOfInterestEntity(
    id = id.toLongOrNull() ?: 0L,
    tripId = tripId,
    name = name,
    address = address,
    isActive = isActive
)
