package com.intimocoffee.waiter.feature.reservations.domain.model

import kotlinx.datetime.*
import java.math.BigDecimal

data class Reservation(
    val id: Long = 0L,
    val tableId: Long,
    val tableName: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val partySize: Int,
    val reservationDate: LocalDateTime,
    val duration: Int = 120, // Duration in minutes, default 2 hours
    val status: ReservationStatus,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val createdBy: Long
) {
    val endTime: LocalDateTime
        get() = LocalDateTime(
            reservationDate.year,
            reservationDate.month,
            reservationDate.dayOfMonth,
            reservationDate.hour,
            reservationDate.minute + duration
        )
    
    val displayName: String
        get() = "$customerName - $tableName"
        
    val isActive: Boolean
        get() = status == ReservationStatus.CONFIRMED || status == ReservationStatus.SEATED
        
    val isPast: Boolean
        get() = endTime < Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

enum class ReservationStatus(val displayName: String) {
    PENDING("Pendiente"),
    CONFIRMED("Confirmada"),
    SEATED("En mesa"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada"),
    NO_SHOW("No se presentó")
}

data class ReservationSlot(
    val time: LocalDateTime,
    val tableId: Long,
    val tableName: String,
    val isAvailable: Boolean,
    val conflictingReservation: Reservation? = null
)