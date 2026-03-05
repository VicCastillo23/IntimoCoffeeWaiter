package com.intimocoffee.waiter.feature.reservations.domain.repository

import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationSlot
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface ReservationRepository {
    fun getAllReservations(): Flow<List<Reservation>>
    fun getReservationsByDate(date: LocalDate): Flow<List<Reservation>>
    fun getReservationsByStatus(status: ReservationStatus): Flow<List<Reservation>>
    fun getActiveReservations(): Flow<List<Reservation>>
    suspend fun getReservationById(id: Long): Reservation?
    suspend fun createReservation(reservation: Reservation): Boolean
    suspend fun updateReservation(reservation: Reservation): Boolean
    suspend fun updateReservationStatus(id: Long, status: ReservationStatus): Boolean
    suspend fun cancelReservation(id: Long): Boolean
    suspend fun getAvailableSlots(date: LocalDate, tableId: Long?, duration: Int): List<ReservationSlot>
    suspend fun checkAvailability(tableId: Long, startTime: LocalDateTime, duration: Int): Boolean
    fun getReservationsByTable(tableId: Long): Flow<List<Reservation>>
    fun getTodaysReservations(): Flow<List<Reservation>>
    fun getUpcomingReservations(): Flow<List<Reservation>>
}