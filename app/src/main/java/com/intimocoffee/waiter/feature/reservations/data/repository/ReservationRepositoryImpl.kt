package com.intimocoffee.waiter.feature.reservations.data.repository

import com.intimocoffee.waiter.core.database.dao.ReservationDao
import com.intimocoffee.waiter.feature.reservations.data.mapper.toDomainModel
import com.intimocoffee.waiter.feature.reservations.data.mapper.toEntity
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationSlot
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import com.intimocoffee.waiter.feature.reservations.domain.repository.ReservationRepository
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReservationRepositoryImpl @Inject constructor(
    private val reservationDao: ReservationDao,
    private val tableRepository: TableRepository
) : ReservationRepository {

    override fun getAllReservations(): Flow<List<Reservation>> {
        return reservationDao.getAllReservations().map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override fun getReservationsByDate(date: LocalDate): Flow<List<Reservation>> {
        return reservationDao.getReservationsByDate(date.toString()).map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override fun getReservationsByStatus(status: ReservationStatus): Flow<List<Reservation>> {
        return reservationDao.getReservationsByStatus(status.name).map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override fun getActiveReservations(): Flow<List<Reservation>> {
        return reservationDao.getAllActiveReservations().map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override suspend fun getReservationById(id: Long): Reservation? {
        val entity = reservationDao.getReservationById(id) ?: return null
        val table = tableRepository.getTableById(entity.tableId)
        return entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
    }

    override suspend fun createReservation(reservation: Reservation): Boolean {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val newReservation = reservation.copy(
                id = 0, // Let Room auto-generate the ID
                createdAt = now,
                updatedAt = now
            )
            
            // Insert reservation into database
            val reservationId = reservationDao.insertReservation(newReservation.toEntity())
            
            // Update table status to RESERVED
            val tableUpdated = tableRepository.updateTableStatus(
                tableId = reservation.tableId,
                status = TableStatus.RESERVED,
                orderId = null
            )
            
            if (!tableUpdated) {
                // If table update failed, remove the reservation
                reservationDao.getReservationById(reservationId)?.let {
                    reservationDao.deleteReservation(it)
                }
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateReservation(reservation: Reservation): Boolean {
        return try {
            val updatedReservation = reservation.copy(
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            
            reservationDao.updateReservation(updatedReservation.toEntity())
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateReservationStatus(id: Long, status: ReservationStatus): Boolean {
        return try {
            val reservation = getReservationById(id) ?: return false
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Update reservation status in database
            reservationDao.updateReservationStatus(id, status.name, now.toString())
            
            // Update table status based on reservation status
            val tableStatus = when (status) {
                ReservationStatus.CONFIRMED -> TableStatus.RESERVED
                ReservationStatus.SEATED -> TableStatus.OCCUPIED
                ReservationStatus.COMPLETED -> TableStatus.FREE
                ReservationStatus.CANCELLED -> TableStatus.FREE
                ReservationStatus.NO_SHOW -> TableStatus.FREE
                else -> TableStatus.RESERVED // PENDING keeps RESERVED
            }
            
            val tableUpdated = tableRepository.updateTableStatus(
                tableId = reservation.tableId,
                status = tableStatus,
                orderId = if (status == ReservationStatus.SEATED) reservation.id else null
            )
            
            if (!tableUpdated) {
                // If table update failed, revert reservation status
                reservationDao.updateReservationStatus(id, reservation.status.name, reservation.updatedAt.toString())
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun cancelReservation(id: Long): Boolean {
        return updateReservationStatus(id, ReservationStatus.CANCELLED)
    }

    override suspend fun getAvailableSlots(date: LocalDate, tableId: Long?, duration: Int): List<ReservationSlot> {
        val slots = mutableListOf<ReservationSlot>()
        val startHour = 11 // 11 AM
        val endHour = 22   // 10 PM
        
        // Use actual tables from repository
        val allTables = tableRepository.getAllActiveTables().first()
        val tables = if (tableId != null) {
            allTables.filter { it.id == tableId }
        } else {
            allTables
        }
        
        for (hour in startHour until endHour step 1) {
            for (table in tables) {
                val slotTime = LocalDateTime(date.year, date.month, date.dayOfMonth, hour, 0)
                val isAvailable = checkAvailability(table.id, slotTime, duration)
                
                slots.add(
                    ReservationSlot(
                        time = slotTime,
                        tableId = table.id,
                        tableName = table.displayName,
                        isAvailable = isAvailable,
                        conflictingReservation = null // We'll calculate this if needed
                    )
                )
            }
        }
        
        return slots
    }

    override suspend fun checkAvailability(tableId: Long, startTime: LocalDateTime, duration: Int): Boolean {
        val endTime = LocalDateTime(
            startTime.year,
            startTime.month,
            startTime.dayOfMonth,
            startTime.hour,
            startTime.minute + duration
        )
        
        val conflictingReservations = reservationDao.getConflictingReservations(
            tableId = tableId,
            startTime = startTime.toString(),
            endTime = endTime.toString()
        )
        
        return conflictingReservations.isEmpty()
    }

    override fun getReservationsByTable(tableId: Long): Flow<List<Reservation>> {
        return reservationDao.getReservationsByTable(tableId).map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override fun getTodaysReservations(): Flow<List<Reservation>> {
        return reservationDao.getTodaysReservations().map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }

    override fun getUpcomingReservations(): Flow<List<Reservation>> {
        return reservationDao.getUpcomingReservations().map { entities ->
            entities.map { entity ->
                val table = tableRepository.getTableById(entity.tableId)
                entity.toDomainModel(table?.displayName ?: "Mesa ${entity.tableId}")
            }
        }
    }
}
