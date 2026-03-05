package com.intimocoffee.waiter.feature.reservations.data.mapper

import com.intimocoffee.waiter.core.database.entity.ReservationEntity
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

fun ReservationEntity.toDomainModel(tableName: String): Reservation {
    return Reservation(
        id = id,
        tableId = tableId,
        tableName = tableName,
        customerName = customerName,
        customerPhone = customerPhone,
        customerEmail = customerEmail,
        partySize = partySize,
        reservationDate = LocalDateTime.parse(reservationDate),
        duration = duration,
        status = ReservationStatus.valueOf(status),
        notes = notes,
        createdAt = LocalDateTime.parse(createdAt),
        updatedAt = LocalDateTime.parse(updatedAt),
        createdBy = createdBy
    )
}

fun Reservation.toEntity(): ReservationEntity {
    return ReservationEntity(
        id = id,
        tableId = tableId,
        customerName = customerName,
        customerPhone = customerPhone ?: "",
        customerEmail = customerEmail,
        partySize = partySize,
        reservationDate = reservationDate.toString(),
        duration = duration,
        status = status.name,
        notes = notes,
        isActive = true, // Always true when creating/updating
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        createdBy = createdBy
    )
}
